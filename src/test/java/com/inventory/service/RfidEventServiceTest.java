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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RfidEventServiceTest {

    @Mock
    private ProductDao productDao;

    @Mock
    private StockDao stockDao;

    @Mock
    private StoreStockDao storeStockDao;

    @Mock
    private RfidEventDao rfidEventDao;

    @Mock
    private ShelfDao shelfDao;

    @Mock
    private ShelfService shelfService;

    @Mock
    private RfidWsService rfidWsService;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private RfidEventService rfidEventService;

    private Product testProduct;
    private Shelf testShelf;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setRfidTag("TEST123");
        testProduct.setUnitWeight(BigDecimal.valueOf(0.5));

        testShelf = new Shelf();
        testShelf.setId(1L);
        testShelf.setName("Test Shelf");
        testShelf.setCurrentWeight(BigDecimal.valueOf(0.0));
    }

    @Test
    void testHandleStoreEntry_WithoutPriorStockExit_ShouldThrowException() {
        // Arrange
        String rfidTag = "TEST123";
        String esp32Id = "ESP32-001";
        Long shelfId = 1L;
        int qty = 5;

        when(productDao.findByRfidTag(rfidTag)).thenReturn(testProduct);
        // Simulate: no previous stock exit event (0 events)
        when(rfidEventDao.hasStockExit(testProduct.getId())).thenReturn(0L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            rfidEventService.handleStoreEntry(rfidTag, esp32Id, shelfId, qty)
        );

        assertEquals("Le produit doit être sorti du stock avant d'entrer en magasin!", exception.getMessage());

        // Verify that alert service was called to create an alert
        verify(alertService, times(1)).createProductWithoutStockExitAlert(testProduct.getId(), testProduct.getName());
        
        // Verify that insert methods were NOT called
        verify(rfidEventDao, never()).insert(any(RfidEvent.class));
        verify(stockDao, never()).decrease(anyLong(), anyInt());
        verify(storeStockDao, never()).insert(any(StoreStock.class));
    }

    @Test
    void testHandleStoreEntry_WithPriorStockExit_ShouldSucceed() {
        // Arrange
        String rfidTag = "TEST123";
        String esp32Id = "ESP32-001";
        Long shelfId = 1L;
        int qty = 5;

        when(productDao.findByRfidTag(rfidTag)).thenReturn(testProduct);
        // Simulate: product has been exited from stock before (1 event)
        when(rfidEventDao.hasStockExit(testProduct.getId())).thenReturn(1L);
        when(rfidEventDao.insert(any(RfidEvent.class))).thenReturn(1);
        when(stockDao.decrease(testProduct.getId(), qty)).thenReturn(1);
        when(storeStockDao.findByProductAndShelf(testProduct.getId(), shelfId)).thenReturn(null);
        when(shelfDao.findById(shelfId)).thenReturn(testShelf);

        // Act
        rfidEventService.handleStoreEntry(rfidTag, esp32Id, shelfId, qty);

        // Assert
        // Verify that the event was inserted
        verify(rfidEventDao, times(1)).insert(any(RfidEvent.class));
        // Verify that stock was decreased
        verify(stockDao, times(1)).decrease(testProduct.getId(), qty);
        // Verify that store stock was created
        verify(storeStockDao, times(1)).insert(any(StoreStock.class));
        // Verify that shelf weight was updated
        verify(shelfService, times(1)).updateShelfWeight(anyLong(), anyDouble());
    }

    @Test
    void testHandleStoreEntry_WithMultiplePriorStockExits_ShouldSucceed() {
        // Arrange
        String rfidTag = "TEST123";
        String esp32Id = "ESP32-001";
        Long shelfId = 1L;
        int qty = 3;

        when(productDao.findByRfidTag(rfidTag)).thenReturn(testProduct);
        // Simulate: product has been exited from stock multiple times (3 events)
        when(rfidEventDao.hasStockExit(testProduct.getId())).thenReturn(3L);
        when(rfidEventDao.insert(any(RfidEvent.class))).thenReturn(1);
        when(stockDao.decrease(testProduct.getId(), qty)).thenReturn(1);
        when(storeStockDao.findByProductAndShelf(testProduct.getId(), shelfId)).thenReturn(null);
        when(shelfDao.findById(shelfId)).thenReturn(testShelf);

        // Act
        rfidEventService.handleStoreEntry(rfidTag, esp32Id, shelfId, qty);

        // Assert
        verify(rfidEventDao, times(1)).insert(any(RfidEvent.class));
        verify(stockDao, times(1)).decrease(testProduct.getId(), qty);
        verify(storeStockDao, times(1)).insert(any(StoreStock.class));
    }

    @Test
    void testHandleStoreEntry_UnknownProduct_ShouldThrowException() {
        // Arrange
        String rfidTag = "UNKNOWN";
        String esp32Id = "ESP32-001";
        Long shelfId = 1L;
        int qty = 5;

        when(productDao.findByRfidTag(rfidTag)).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            rfidEventService.handleStoreEntry(rfidTag, esp32Id, shelfId, qty)
        );

        assertEquals("Produit inconnu.", exception.getMessage());

        // Verify that hasStockExit was never called
        verify(rfidEventDao, never()).hasStockExit(anyLong());
    }

    @Test
    void testHandleStockExit_ShouldAllowProductExit() {
        // Arrange
        String rfidTag = "TEST123";
        String esp32Id = "ESP32-001";
        int qty = 5;

        when(productDao.findByRfidTag(rfidTag)).thenReturn(testProduct);
        when(stockDao.decrease(testProduct.getId(), qty)).thenReturn(1);
        when(rfidEventDao.insert(any(RfidEvent.class))).thenReturn(1);

        // Act
        rfidEventService.handleStockExit(rfidTag, esp32Id, qty);

        // Assert
        verify(rfidEventDao, times(1)).insert(any(RfidEvent.class));
        verify(stockDao, times(1)).decrease(testProduct.getId(), qty);
    }

}
