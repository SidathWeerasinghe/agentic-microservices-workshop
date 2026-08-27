package com.example.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private String orderId;
    private String customerId;
    private String status;

    private StockCheckResult stockCheckResult;
    private ProductionOrderResult productionOrderResult;
    private ShippingQuoteResult shippingQuoteResult;
    private PaymentResult paymentResult;
    private ShippingConfirmationResult shippingConfirmationResult;

    private BigDecimal totalAmount;
    private BigDecimal shippingCost;
    private BigDecimal grandTotal;

    private String failureReason;
    private String failedStep;
    private LocalDateTime createdAt;

}
