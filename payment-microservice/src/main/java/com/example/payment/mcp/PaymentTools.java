package com.example.payment.mcp;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentRetryRequest;
import com.example.payment.service.PaymentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the payment domain.
 * <p>
 * Thin wrappers over the existing {@link PaymentService} bean - the same
 * business logic the REST controller uses. The REST controller remains
 * untouched.
 * <p>
 * Tool arguments come from an LLM, so they are validated here with the same Bean
 * Validation rules the REST layer enforces with {@code @Valid}.
 */
@Component
@RequiredArgsConstructor
public class PaymentTools {

    private final PaymentService paymentService;
    private final Validator validator;

    @Tool(description = "Process a new payment for an order. Write operation. "
            + "Requires orderId, amount (positive, in LKR), currency (LKR) and paymentMethod "
            + "(one of CARD, CASH, BANK_TRANSFER). Returns the payment with its paymentReference and status. "
            + "Only one payment is allowed per order: if the status is FAILED, call retryPayment with the "
            + "paymentReference - do NOT call makePayment again for the same order.")
    public PaymentResponse makePayment(
            @ToolParam(description = "Payment details: orderId, amount (LKR), currency (LKR), "
                    + "paymentMethod (CARD|CASH|BANK_TRANSFER), optional customerId")
            PaymentRequest request) {
        return paymentService.makePayment(validate(request));
    }

    @Tool(description = "Retry a previously FAILED payment using its paymentReference. Write operation. "
            + "Optionally override the payment method. Fails if the payment already succeeded.")
    public PaymentResponse retryPayment(
            @ToolParam(description = "The paymentReference of the failed payment to retry") String paymentReference,
            @ToolParam(description = "Optional retry details: paymentMethod (CARD|CASH|BANK_TRANSFER) and reason. "
                    + "May be null to retry as-is.", required = false)
            PaymentRetryRequest request) {
        requireText(paymentReference, "paymentReference");
        return paymentService.retryPayment(paymentReference, request);
    }

    @Tool(description = "Retrieve the details and current status of a payment by its paymentReference.")
    public PaymentResponse getPayment(
            @ToolParam(description = "The paymentReference to look up") String paymentReference) {
        requireText(paymentReference, "paymentReference");
        return paymentService.getPayment(paymentReference);
    }

    private <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
