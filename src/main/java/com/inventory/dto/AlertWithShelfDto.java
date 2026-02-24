package com.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertWithShelfDto {
    private Long id;
    private Long shelfId;
    private String shelfName;
    private Long productId;
    private String productName;
    private String alertType;
    private String status;
    private String createdAt;
}
