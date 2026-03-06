package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.StoreStockWithDetailsDto;
import com.inventory.service.StoreStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store-stock")

public class StoreController {
	@Autowired
	private StoreStockService storeStockService;

	@GetMapping
	public ApiResponse<List<StoreStockWithDetailsDto>> getAllStoreStock() {
		return storeStockService.getAllStoreStockWithDetails();
	}

	@DeleteMapping("/{id}")
	public ApiResponse<Void> deleteStoreStock(@PathVariable Long id) {
		storeStockService.deleteStoreStockById(id);
		return ApiResponse.ok("STORE_STOCK_DELETED", null);
	}
}
