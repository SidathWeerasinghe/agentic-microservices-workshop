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
                int requested = requestedQuantity(request, stockResult.getItemId());
                String errorMessage = String.format("Insufficient stock: requested %d but only %d available at branch %s",
                        requested, stockResult.getAvailableQuantity(), request.getBranchId());
                log.warn("Order rejected - {}", errorMessage);
                throw new OrchestrationException(
                        errorMessage,
                        "STOCK_CHECK",
                        responseBuilder.status("INSUFFICIENT_STOCK").failureReason(errorMessage).failedStep("STOCK_CHECK").build()
                );
            }

            // Step 2: Create a production order per item - allocates the ordered quantity
            // from branch stock (inventory deducts it, and rejects it if stock ran out meanwhile)
            log.info("Step 2: Creating production orders to allocate stock for {} items", request.getItems().size());
            ProductionOrderResult productionResult = createProductionOrders(orderId, request);
            responseBuilder.productionOrderResult(productionResult);
            log.info("Production order(s) created, last: {}", productionResult.getProductionOrderId());

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

    /**
     * Checks stock for EVERY item in the order and sets each item's unit price from the
     * inventory response. Returns the first item that cannot be fulfilled, or the first
     * item's result when all items are sufficient.
     */
    private StockCheckResult checkStockForItems(OrderRequest request) {
        StockCheckResult firstResult = null;
        for (OrderItem item : request.getItems()) {
            StockCheckResult result = inventoryClient.checkStock(item.getItemId(), request.getBranchId());

            if (result.getUnitPrice() != null) {
                item.setUnitPrice(result.getUnitPrice());
            }

            int available = result.getAvailableQuantity() != null ? result.getAvailableQuantity() : 0;
            int requested = requestedQuantity(request, item.getItemId());
            result.setSufficient(available >= requested);

            if (!result.isSufficient()) {
                return result;
            }
            if (firstResult == null) {
                firstResult = result;
            }
        }
        return firstResult;
    }

    /** Total quantity requested for an item (the same item may appear on more than one line). */
    private int requestedQuantity(OrderRequest request, String itemId) {
        return request.getItems().stream()
                .filter(item -> item.getItemId().equals(itemId))
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    private ProductionOrderResult createProductionOrders(String orderId, OrderRequest request) {
        ProductionOrderResult lastResult = null;
        for (OrderItem item : request.getItems()) {
            lastResult = inventoryClient.createProductionOrder(
                    item.getItemId(),
                    request.getBranchId(),
                    item.getQuantity(),
                    "Order " + orderId + " for customer " + request.getCustomerId()
            );
        }
        return lastResult;
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
        if (response.getProductionOrderResult() == null) return "PRODUCTION_ORDER";
        if (response.getShippingQuoteResult() == null) return "SHIPPING_QUOTE";
        if (response.getPaymentResult() == null) return "PAYMENT";
        if (response.getShippingConfirmationResult() == null) return "SHIPPING_CONFIRMATION";
        return "UNKNOWN";
    }

}
