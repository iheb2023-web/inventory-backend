package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreStockWithDetailsDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productBarcode;
    private Long shelfId;
    private String shelfName;
    private Integer quantity;
    private BigDecimal unitWeight;
    private String lastUpdated;
}
