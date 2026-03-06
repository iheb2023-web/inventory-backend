package com.inventory.service;

import com.inventory.dao.ProductDao;
import com.inventory.dao.StoreStockDao;
import com.inventory.dao.ShelfDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.StoreStockWithDetailsDto;
import com.inventory.model.product.Product;
import com.inventory.model.product.StoreStock;
import com.inventory.model.store.Shelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StoreStockService {
    @Autowired
    private StoreStockDao storeStockDao;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ShelfDao shelfDao;

    @Autowired
    private ShelfService shelfService;

    public ApiResponse<List<StoreStockWithDetailsDto>> getAllStoreStockWithDetails() {
        try {
            List<StoreStockWithDetailsDto> storeStockList = storeStockDao.findAllWithDetails();
            return ApiResponse.ok("Store stock retrieved successfully", storeStockList);
        } catch (Exception e) {
            return ApiResponse.fail("Error retrieving store stock: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteStoreStockById(Long id) {
        StoreStock storeStock = storeStockDao.findById(id);
        if (storeStock == null) {
            throw new RuntimeException("Entrée stock magasin introuvable.");
        }

        int deleted = storeStockDao.deleteById(id);
        if (deleted == 0) {
            throw new RuntimeException("Suppression impossible pour cette entrée du stock magasin.");
        }

        Product product = productDao.findById(storeStock.getProductId());
        Shelf shelf = shelfDao.findById(storeStock.getShelfId());
        if (product != null && shelf != null && storeStock.getQuantity() != null && storeStock.getQuantity() > 0) {
            double removedWeight = product.getUnitWeight().doubleValue() * storeStock.getQuantity();
            double newWeight = Math.max(0, shelf.getCurrentWeight().doubleValue() - removedWeight);
            shelfService.updateShelfWeight(shelf.getId(), newWeight);
        }
    }
}
