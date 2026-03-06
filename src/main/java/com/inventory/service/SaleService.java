package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.dao.SaleDao;
import com.inventory.dao.StoreStockDao;
import com.inventory.dto.SaleTransactionDto;
import com.inventory.model.product.Product;
import com.inventory.model.product.Sale;
import com.inventory.model.product.StoreStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final ProductDao productDao;
    private final StoreStockDao storeStockDao;
    private final SaleDao saleDao;

    @Transactional
    public void makeSaleByBarcode(String barcode, Long shelfId, int qty, BigDecimal totalPrice) {

        Product product = productDao.findByBarcode(barcode);
        if (product == null) throw new RuntimeException("Produit introuvable");

        int updated = storeStockDao.decrease(product.getId(), shelfId, qty);
        if (updated == 0) throw new RuntimeException("Stock magasin insuffisant!");

        Sale sale = new Sale();
        sale.setProductId(product.getId());
        sale.setQuantity(qty);
        sale.setTotalPrice(totalPrice);
        saleDao.insert(sale);
    }

    @Transactional
    public void makeSale(Long productId, int quantity) {
        Product product = productDao.findById(productId);
        if (product == null) {
            throw new RuntimeException("Produit introuvable avec l'ID: " + productId);
        }

        // Find all store stocks for this product
        List<StoreStock> storeStocks = storeStockDao.findAll().stream()
                .filter(ss -> ss.getProductId().equals(productId))
                .toList();

        if (storeStocks.isEmpty()) {
            throw new RuntimeException("Aucun stock disponible en magasin pour ce produit");
        }

        // Calculate total available quantity
        int totalAvailable = storeStocks.stream()
                .mapToInt(StoreStock::getQuantity)
                .sum();

        if (totalAvailable < quantity) {
            throw new RuntimeException("Stock insuffisant. Disponible: " + totalAvailable + ", Demandé: " + quantity);
        }

        // Decrease stock from shelves (starting with the shelf that has the most stock)
        int remainingToDecrease = quantity;
        List<StoreStock> sortedStocks = storeStocks.stream()
                .sorted((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()))
                .toList();

        for (StoreStock storeStock : sortedStocks) {
            if (remainingToDecrease <= 0) break;

            int toDecrease = Math.min(remainingToDecrease, storeStock.getQuantity());
            int updated = storeStockDao.decrease(productId, storeStock.getShelfId(), toDecrease);
            
            if (updated == 0) {
                throw new RuntimeException("Erreur lors de la mise à jour du stock");
            }
            
            remainingToDecrease -= toDecrease;
        }

        // Record the sale
        Sale sale = new Sale();
        sale.setProductId(productId);
        sale.setQuantity(quantity);
        sale.setTotalPrice(BigDecimal.ZERO); // Price can be null or zero for admin sales
        saleDao.insert(sale);
    }

    @Transactional
    public void makeMultipleSale(List<com.inventory.dto.SaleItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("La liste des articles est vide");
        }

        // Validate all items first
        for (com.inventory.dto.SaleItemRequest item : items) {
            Product product = productDao.findById(item.getProductId());
            if (product == null) {
                throw new RuntimeException("Produit introuvable avec l'ID: " + item.getProductId());
            }

            // Check stock availability in store
            List<StoreStock> storeStocks = storeStockDao.findAll().stream()
                    .filter(ss -> ss.getProductId().equals(item.getProductId()))
                    .toList();

            if (storeStocks.isEmpty()) {
                throw new RuntimeException("Produit '" + product.getName() + "' non disponible en magasin");
            }

            int totalAvailable = storeStocks.stream()
                    .mapToInt(StoreStock::getQuantity)
                    .sum();

            if (totalAvailable < item.getQuantity()) {
                throw new RuntimeException("Stock insuffisant pour '" + product.getName() + "'. Disponible: " + totalAvailable + ", Demandé: " + item.getQuantity());
            }
        }

        // Process all sales
        for (com.inventory.dto.SaleItemRequest item : items) {
            int quantity = item.getQuantity();
            Long productId = item.getProductId();

            // Find all store stocks for this product
            List<StoreStock> storeStocks = storeStockDao.findAll().stream()
                    .filter(ss -> ss.getProductId().equals(productId))
                    .sorted((a, b) -> Integer.compare(b.getQuantity(), a.getQuantity()))
                    .toList();

            // Decrease stock from shelves
            int remainingToDecrease = quantity;
            for (StoreStock storeStock : storeStocks) {
                if (remainingToDecrease <= 0) break;

                int toDecrease = Math.min(remainingToDecrease, storeStock.getQuantity());
                int updated = storeStockDao.decrease(productId, storeStock.getShelfId(), toDecrease);

                if (updated == 0) {
                    throw new RuntimeException("Erreur lors de la mise à jour du stock");
                }

                remainingToDecrease -= toDecrease;
            }

            // Record the sale
            Sale sale = new Sale();
            sale.setProductId(productId);
            sale.setQuantity(quantity);
            sale.setTotalPrice(item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
            saleDao.insert(sale);
        }
    }

    public List<SaleTransactionDto> getRecentSalesGrouped(int limit) {
        // Fetch recent sales
        List<Sale> sales = saleDao.findRecentSales(limit * 3); // Fetch more to account for grouping
        
        // Group sales by minute (same minute = same transaction)
        Map<LocalDateTime, List<Sale>> groupedByMinute = new TreeMap<>(Collections.reverseOrder());
        
        for (Sale sale : sales) {
            LocalDateTime minuteKey = sale.getSoldAt().truncatedTo(ChronoUnit.MINUTES);
            groupedByMinute.computeIfAbsent(minuteKey, k -> new ArrayList<>()).add(sale);
        }
        
        // Convert to SaleTransactionDto
        List<SaleTransactionDto> transactions = new ArrayList<>();
        int count = 0;
        
        for (Map.Entry<LocalDateTime, List<Sale>> entry : groupedByMinute.entrySet()) {
            if (count >= limit) break;
            
            LocalDateTime transactionDate = entry.getKey();
            List<Sale> salesInTransaction = entry.getValue();
            
            SaleTransactionDto transaction = new SaleTransactionDto();
            transaction.setTransactionDate(transactionDate);
            
            // Build sale items with product details
            List<SaleTransactionDto.SaleItemDto> items = new ArrayList<>();
            int totalQuantity = 0;
            BigDecimal totalPrice = BigDecimal.ZERO;
            
            for (Sale sale : salesInTransaction) {
                Product product = productDao.findById(sale.getProductId());
                
                SaleTransactionDto.SaleItemDto itemDto = new SaleTransactionDto.SaleItemDto();
                itemDto.setSaleId(sale.getId());
                itemDto.setProductId(sale.getProductId());
                itemDto.setProductName(product != null ? product.getName() : "Produit inconnu");
                itemDto.setQuantity(sale.getQuantity());
                itemDto.setTotalPrice(sale.getTotalPrice());
                
                if (product != null && sale.getQuantity() > 0) {
                    BigDecimal unitPrice = sale.getTotalPrice().divide(
                            new BigDecimal(sale.getQuantity()), 
                            2, 
                            BigDecimal.ROUND_HALF_UP
                    );
                    itemDto.setUnitPrice(unitPrice);
                } else {
                    itemDto.setUnitPrice(BigDecimal.ZERO);
                }
                
                items.add(itemDto);
                totalQuantity += sale.getQuantity();
                totalPrice = totalPrice.add(sale.getTotalPrice());
            }
            
            transaction.setItems(items);
            transaction.setTotalQuantity(totalQuantity);
            transaction.setTotalPrice(totalPrice);
            
            transactions.add(transaction);
            count++;
        }
        
        return transactions;
    }
}
