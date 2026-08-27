package com.example.inventory.dto;

import com.example.inventory.enums.StockStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCatalogResponse {

    private String itemId;
    private String itemName;
    private List<BranchStock> branches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchStock {
        private String branchId;
        private String branchName;
        private Integer availableQuantity;
        private BigDecimal unitPrice;
        private StockStatus status;
    }

}
