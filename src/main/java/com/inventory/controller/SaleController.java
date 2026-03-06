package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.SaleQrRequest;
import com.inventory.dto.SaleRequest;
import com.inventory.dto.MultipleSaleRequest;
import com.inventory.dto.SaleTransactionDto;
import com.inventory.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping("/qr")
    public ApiResponse<Void> saleByQr(@RequestBody SaleQrRequest req) {

        int qty = (req.getQty() == null || req.getQty() <= 0) ? 1 : req.getQty();

        saleService.makeSaleByBarcode(req.getBarcode(), req.getShelfId(), qty, req.getTotalPrice());
        return ApiResponse.ok("SALE_CREATED", null);
    }

    @PostMapping
    public ApiResponse<Void> createSale(@RequestBody SaleRequest req) {
        if (req.getProductId() == null) {
            throw new RuntimeException("Product ID is required");
        }
        
        int quantity = (req.getQuantity() == null || req.getQuantity() <= 0) ? 1 : req.getQuantity();
        
        saleService.makeSale(req.getProductId(), quantity);
        return ApiResponse.ok("SALE_CREATED", null);
    }

    @PostMapping("/multiple")
    public ApiResponse<Void> createMultipleSale(@RequestBody MultipleSaleRequest req) {
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new RuntimeException("Items list is required");
        }
        
        saleService.makeMultipleSale(req.getItems());
        return ApiResponse.ok("SALES_CREATED", null);
    }

    @GetMapping("/grouped")
    public ApiResponse<List<SaleTransactionDto>> getRecentSalesGrouped(@RequestParam(defaultValue = "50") int limit) {
        List<SaleTransactionDto> transactions = saleService.getRecentSalesGrouped(limit);
        return ApiResponse.ok("RECENT_SALES_GROUPED", transactions);
    }
}