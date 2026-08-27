package com.example.inventory.dto;

import com.example.inventory.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionOrderResponse {

    private String productionOrderId;
    private OrderStatus status;

}
