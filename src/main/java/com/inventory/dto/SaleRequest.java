package com.inventory.dto;

import lombok.Data;

@Data
public class SaleRequest {
    private Long productId;
    private Integer quantity;
}
