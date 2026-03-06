package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.dao.RfidEventDao;
import com.inventory.dao.ShelfDao;
import com.inventory.dao.StockDao;
import com.inventory.dao.StoreStockDao;
import com.inventory.model.device.RfidEvent;
import com.inventory.model.product.Product;
import com.inventory.model.product.Stock;
import com.inventory.model.product.StoreStock;
import com.inventory.model.store.Shelf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RfidEventService {

    private final ProductDao productDao;
    private final StockDao stockDao;
    private final StoreStockDao storeStockDao;
    private final RfidEventDao rfidEventDao;
    private final ShelfDao shelfDao;
    private final ShelfService shelfService;
    private final RfidWsService rfidWsService;
    private final AlertService alertService;

    public List<RfidEvent> getAllEvents() {
        return rfidEventDao.findAll();
    }

    @Transactional
    public void handleStockEntry(String rfidTag, String esp32Id, int qty) {

        log.info("🔍 handleStockEntry called: rfidTag={}, esp32Id={}, qty={}", rfidTag, esp32Id, qty);
        
        Product product = productDao.findByRfidTag(rfidTag);
        if (product == null) {
            log.warn("❌ Unknown product detected with RFID tag: {}", rfidTag);
            // Create an alert for unknown product
            alertService.createProductWithoutStockAlert(null, "Tag RFID: " + rfidTag);
            log.info("✅ Alert sent, now triggering NEW_PRODUCT notification");
            rfidWsService.notifyNewProduct(rfidTag, "STOCK");
            return;
        }

        log.info("✅ Product found: {}", product.getId());
        RfidEvent event = new RfidEvent();
        event.setProductId(product.getId());
        event.setEventType(RfidEvent.EventType.ENTRY);
        event.setLocation(RfidEvent.EventLocation.STOCK);
        event.setEsp32Id(esp32Id);
        rfidEventDao.insert(event);

        Stock stock = stockDao.findByProductId(product.getId());
        if (stock == null) {
            Stock s = new Stock();
            s.setProductId(product.getId());
            s.setQuantity(qty);
            stockDao.insert(s);
        } else {
            stockDao.increase(product.getId(), qty);
        }
    }


    @Transactional
    public void handleStockExit(String rfidTag, String esp32Id, int qty) {

        Product product = productDao.findByRfidTag(rfidTag);
        if (product == null) {
            alertService.createProductWithoutStockAlert(null, "Tag RFID: " + rfidTag);
            throw new RuntimeException("Produit inconnu.");
        }

        RfidEvent event = new RfidEvent();
        event.setProductId(product.getId());
        event.setEventType(RfidEvent.EventType.EXIT);
        event.setLocation(RfidEvent.EventLocation.STOCK);
        event.setEsp32Id(esp32Id);
        rfidEventDao.insert(event);

        int updated = stockDao.decrease(product.getId(), qty);
        if (updated == 0) {
            throw new RuntimeException("Stock insuffisant!");
        }
    }


    @Transactional
    public void handleStoreEntry(String rfidTag, String esp32Id, Long shelfId, int qty) {

        Product product = productDao.findByRfidTag(rfidTag);
        if (product == null) {
            alertService.createProductWithoutStockAlert(null, "Tag RFID: " + rfidTag);
            throw new RuntimeException("Produit inconnu.");
        }

        // Verify that the product has been previously exited from stock
        long hasExit = rfidEventDao.hasStockExit(product.getId());
        if (hasExit == 0) {
            // Create an alert instead of throwing an exception
            alertService.createProductWithoutStockExitAlert(product.getId(), product.getName());
            throw new RuntimeException("Le produit doit être sorti du stock avant d'entrer en magasin!");
        }

        RfidEvent event = new RfidEvent();
        event.setProductId(product.getId());
        event.setEventType(RfidEvent.EventType.ENTRY);
        event.setLocation(RfidEvent.EventLocation.STORE);
        event.setEsp32Id(esp32Id);
        rfidEventDao.insert(event);

        int updated = stockDao.decrease(product.getId(), qty);
        if (updated == 0) {
            throw new RuntimeException("Stock insuffisant pour transfert vers magasin!");
        }

        StoreStock ss = storeStockDao.findByProductAndShelf(product.getId(), shelfId);
        if (ss == null) {
            StoreStock newSS = new StoreStock();
            newSS.setProductId(product.getId());
            newSS.setShelfId(shelfId);
            newSS.setQuantity(qty);
            storeStockDao.insert(newSS);
        } else {
            storeStockDao.increase(product.getId(), shelfId, qty);
        }

        // Update shelf weight with added product
        Shelf shelf = shelfDao.findById(shelfId);
        if (shelf != null) {
            double productWeight = product.getUnitWeight().doubleValue() * qty;
            double newShelfWeight = shelf.getCurrentWeight().doubleValue() + productWeight;
            shelfService.updateShelfWeight(shelfId, newShelfWeight);
        }
    }

    @Transactional
    public void handleStoreExit(String rfidTag, String esp32Id, Long shelfId, int qty) {

        Product product = productDao.findByRfidTag(rfidTag);
        if (product == null) {
            alertService.createProductWithoutStockAlert(null, "Tag RFID: " + rfidTag);
            throw new RuntimeException("Produit inconnu.");
        }

        RfidEvent event = new RfidEvent();
        event.setProductId(product.getId());
        event.setEventType(RfidEvent.EventType.EXIT);
        event.setLocation(RfidEvent.EventLocation.STORE);
        event.setEsp32Id(esp32Id);
        rfidEventDao.insert(event);

        StoreStock ss = storeStockDao.findByProductAndShelf(product.getId(), shelfId);
        if (ss == null || ss.getQuantity() < qty) {
            throw new RuntimeException("Quantité insuffisante en magasin!");
        }

        storeStockDao.decrease(product.getId(), shelfId, qty);
        stockDao.increase(product.getId(), qty);

        // Update shelf weight with removed product
        Shelf shelf = shelfDao.findById(shelfId);
        if (shelf != null) {
            double productWeight = product.getUnitWeight().doubleValue() * qty;
            double newShelfWeight = Math.max(0, shelf.getCurrentWeight().doubleValue() - productWeight);
            shelfService.updateShelfWeight(shelfId, newShelfWeight);
        }
    }
    @Transactional
    public void deleteEvent(Long id) {
        rfidEventDao.delete(id);
    }

}