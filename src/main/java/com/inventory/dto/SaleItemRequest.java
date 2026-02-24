package com.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SaleItemRequest {
    private Long productId;
    private Integer quantity;
    private BigDecimal totalPrice;
}
