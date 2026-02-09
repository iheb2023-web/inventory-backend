package com.inventory.service;

import com.inventory.dao.StoreStockDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.StoreStockWithDetailsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreStockService {
    @Autowired
    private StoreStockDao storeStockDao;

    public ApiResponse<List<StoreStockWithDetailsDto>> getAllStoreStockWithDetails() {
        try {
            List<StoreStockWithDetailsDto> storeStockList = storeStockDao.findAllWithDetails();
            return ApiResponse.ok("Store stock retrieved successfully", storeStockList);
        } catch (Exception e) {
            return ApiResponse.fail("Error retrieving store stock: " + e.getMessage());
        }
    }
}
