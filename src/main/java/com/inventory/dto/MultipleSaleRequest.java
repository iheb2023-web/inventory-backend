package com.inventory.dto;

import lombok.Data;
import java.util.List;

@Data
public class MultipleSaleRequest {
    private List<SaleItemRequest> items;
}
