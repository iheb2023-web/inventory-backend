package com.inventory.dto;

import lombok.Data;

@Data
public class SaleItemRequest {
    private Long productId;
    private Integer quantity;
}
