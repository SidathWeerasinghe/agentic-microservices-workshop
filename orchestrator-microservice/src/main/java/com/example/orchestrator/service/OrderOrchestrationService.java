package com.example.orchestrator.service;

import com.example.orchestrator.client.InventoryServiceClient;
import com.example.orchestrator.client.PaymentServiceClient;
import com.example.orchestrator.client.ShippingServiceClient;
import com.example.orchestrator.dto.*;
import com.example.orchestrator.exception.OrchestrationException;
import com.example.orchestrator.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderOrchestrationService {

    private final InventoryServiceClient inventoryClient;
    private final PaymentServiceClient paymentClient;
    private final ShippingServiceClient shippingClient;

    private static final int MAX_PAYMENT_RETRIES = 2;

    public OrderResponse processOrder(OrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Starting order orchestration for orderId: {}", orderId);

        OrderResponse.OrderResponseBuilder responseBuilder = OrderResponse.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .createdAt(LocalDateTime.now());

        try {
            // Step 1: Check Stock for all items
            log.info("Step 1: Checking stock for {} items", request.getItems().size());
            StockCheckResult stockResult = checkStockForItems(request);
            responseBuilder.stockCheckResult(stockResult);
            log.info("Stock check completed: {} (sufficient: {})", stockResult.getStatus(), stockResult.isSufficient());

            // Reject order if stock is insufficient
            if (!stockResult.isSufficient()) {
                int totalRequested = request.getItems().stream()
                        .mapToInt(OrderItem::getQuantity)
                        .sum();
                String errorMessage = String.format("Insufficient stock: requested %d but only %d available at branch %s",
                        totalRequested, stockResult.getAvailableQuantity(), request.getBranchId());
                log.warn("Order rejected - {}", errorMessage);
                throw new OrchestrationException(
                        errorMessage,
                        "STOCK_CHECK",
                        responseBuilder.status("INSUFFICIENT_STOCK").failureReason(errorMessage).failedStep("STOCK_CHECK").build()
                );
            }

            // Step 2: Create Production Order if stock is low (but still sufficient for this order)
            if ("LOW_STOCK".equals(stockResult.getStatus())) {
                log.info("Step 2: Creating production order due to {}", stockResult.getStatus());
                ProductionOrderResult productionResult = createProductionOrder(request, stockResult);
                responseBuilder.productionOrderResult(productionResult);
                log.info("Production order created: {}", productionResult.getProductionOrderId());
            } else {
                log.info("Step 2: Skipped - Stock level is adequate");
            }

            // Step 3: Calculate Shipping Quote
            log.info("Step 3: Calculating shipping quote");
            ShippingQuoteResult shippingQuote = calculateShipping(request);
            responseBuilder.shippingQuoteResult(shippingQuote);
            log.info("Shipping quote calculated: {} {}", shippingQuote.getShippingCost(), shippingQuote.getCurrency());

            // Calculate total amount
            BigDecimal itemsTotal = calculateItemsTotal(request);
            BigDecimal grandTotal = itemsTotal.add(shippingQuote.getShippingCost());
            responseBuilder.totalAmount(itemsTotal);
            responseBuilder.shippingCost(shippingQuote.getShippingCost());
            responseBuilder.grandTotal(grandTotal);

            // Step 4: Make Payment
            log.info("Step 4: Processing payment for amount: {}", grandTotal);
            PaymentResult paymentResult = processPayment(orderId, grandTotal, request);
            responseBuilder.paymentResult(paymentResult);

            if (!"SUCCESS".equals(paymentResult.getStatus())) {
                throw new OrchestrationException(
                        "Payment failed: " + paymentResult.getFailureReason(),
                        "PAYMENT",
                        responseBuilder.status("PAYMENT_FAILED").failureReason(paymentResult.getFailureReason()).build()
                );
            }
            log.info("Payment successful: {}", paymentResult.getPaymentReference());

            // Step 5: Confirm Shipping
            log.info("Step 5: Confirming shipping");
            ShippingConfirmationResult shippingConfirmation = confirmShipping(
                    shippingQuote.getQuoteId(), orderId, request.getCustomerId());
            responseBuilder.shippingConfirmationResult(shippingConfirmation);
            log.info("Shipping confirmed: {}", shippingConfirmation.getShipmentId());

            // Build successful response
            OrderResponse response = responseBuilder
                    .status("COMPLETED")
                    .build();

            log.info("Order {} completed successfully", orderId);
            return response;

        } catch (OrchestrationException e) {
            throw e;
        } catch (ServiceException e) {
            String failedStep = determineFailedStep(responseBuilder.build());
            throw new OrchestrationException(e.getMessage(), failedStep, 
                    responseBuilder.status("FAILED").failureReason(e.getMessage()).failedStep(failedStep).build(), e);
        } catch (Exception e) {
            log.error("Unexpected error during orchestration: ", e);
            String failedStep = determineFailedStep(responseBuilder.build());
            throw new OrchestrationException("Unexpected error: " + e.getMessage(), failedStep,
                    responseBuilder.status("FAILED").failureReason(e.getMessage()).failedStep(failedStep).build(), e);
        }
    }

    private StockCheckResult checkStockForItems(OrderRequest request) {
        OrderItem firstItem = request.getItems().get(0);
        StockCheckResult result = inventoryClient.checkStock(firstItem.getItemId(), request.getBranchId());
        
        if (result.getUnitPrice() != null) {
            firstItem.setUnitPrice(result.getUnitPrice());
        }
        
        int totalRequested = request.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        
        result.setSufficient(result.getAvailableQuantity() >= totalRequested);
        return result;
    }

    private ProductionOrderResult createProductionOrder(OrderRequest request, StockCheckResult stockResult) {
        int totalNeeded = request.getItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
        
        int shortfall = totalNeeded - stockResult.getAvailableQuantity();
        
        return inventoryClient.createProductionOrder(
                stockResult.getItemId(),
                request.getBranchId(),
                Math.max(shortfall, 10),
                "Order " + request.getCustomerId() + " - Stock replenishment"
        );
    }

    private ShippingQuoteResult calculateShipping(OrderRequest request) {
        return shippingClient.calculateShippingQuote(
                request.getBranchId(),
                request.getShippingAddress(),
                request.getItems()
        );
    }

    private BigDecimal calculateItemsTotal(OrderRequest request) {
        return request.getItems().stream()
                .filter(item -> item.getUnitPrice() != null)
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentResult processPayment(String orderId, BigDecimal amount, OrderRequest request) {
        PaymentResult result = paymentClient.makePayment(
                orderId,
                amount,
                request.getPaymentDetails().getCurrency(),
                request.getPaymentDetails().getPaymentMethod(),
                request.getCustomerId()
        );

        int retryCount = 0;
        while (!"SUCCESS".equals(result.getStatus()) && retryCount < MAX_PAYMENT_RETRIES) {
            log.warn("Payment attempt {} failed, retrying...", retryCount + 1);
            retryCount++;
            result = paymentClient.retryPayment(result.getPaymentReference(), null);
        }

        return result;
    }

    private ShippingConfirmationResult confirmShipping(String quoteId, String orderId, String customerId) {
        return shippingClient.confirmShipping(quoteId, orderId, customerId);
    }

    private String determineFailedStep(OrderResponse response) {
        if (response.getStockCheckResult() == null) return "STOCK_CHECK";
        if (response.getShippingQuoteResult() == null) return "SHIPPING_QUOTE";
        if (response.getPaymentResult() == null) return "PAYMENT";
        if (response.getShippingConfirmationResult() == null) return "SHIPPING_CONFIRMATION";
        return "UNKNOWN";
    }

}
