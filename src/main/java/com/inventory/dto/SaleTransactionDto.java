package com.inventory.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleTransactionDto {
    private LocalDateTime transactionDate;
    private List<SaleItemDto> items;
    private Integer totalQuantity;
    private BigDecimal totalPrice;
    
    @Data
    public static class SaleItemDto {
        private Long saleId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}
