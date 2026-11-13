package com.example.shipping.mcp;

import com.example.shipping.dto.ShippingConfirmationRequest;
import com.example.shipping.dto.ShippingConfirmationResponse;
import com.example.shipping.dto.ShippingQuoteRequest;
import com.example.shipping.dto.ShippingQuoteResponse;
import com.example.shipping.service.ShippingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the shipping domain.
 * <p>
 * Thin wrappers over the existing {@link ShippingService} bean - the same
 * business logic the REST controller uses. The REST controller remains
 * untouched.
 * <p>
 * Tool arguments come from an LLM, so they are validated here with the same Bean
 * Validation rules the REST layer enforces with {@code @Valid}.
 */
@Component
@RequiredArgsConstructor
public class ShippingTools {

    private final ShippingService shippingService;
    private final Validator validator;

    @Tool(description = "Calculate a shipping cost quote (LKR) for items shipped from a branch to a "
            + "destination address. Cost depends on the distance from the branch's city to the destination city. "
            + "Returns a quoteId that is needed later to confirm shipping.")
    public ShippingQuoteResponse calculateShippingQuote(
            @ToolParam(description = "Quote request: branchId (e.g. BRANCH-COL), destinationAddress (line1, city, "
                    + "area, country, optional postalCode), and items (each with itemId and quantity). "
                    + "Leave weightKg out unless the customer gave a weight - never estimate it.")
            ShippingQuoteRequest request) {
        return shippingService.calculateShippingQuote(validate(request));
    }

    @Tool(description = "Confirm shipping using a previously calculated quote. Write operation. "
            + "Call only after the payment succeeded. Fails if the quote does not exist, was already used, "
            + "or has expired. Requires quoteId, orderId and customerId.")
    public ShippingConfirmationResponse confirmShipping(
            @ToolParam(description = "Confirmation request: quoteId (from calculateShippingQuote), orderId, and customerId")
            ShippingConfirmationRequest request) {
        return shippingService.confirmShipping(validate(request));
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
}
