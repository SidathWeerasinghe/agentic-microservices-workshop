package com.example.orchestrator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResult {

    private String itemId;
    private String branchId;
    private Integer availableQuantity;
    private BigDecimal unitPrice;
    private String status;
    private boolean sufficient;

}
