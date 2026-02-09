package com.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShelfRequest {
    private String name;
    private BigDecimal maxWeight;
    private BigDecimal minThreshold;
}
