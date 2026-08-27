package com.example.inventory.dto;

import com.example.inventory.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {

    private String itemId;
    private String branchId;
    private Integer availableQuantity;
    private BigDecimal unitPrice;
    private StockStatus status;

}
