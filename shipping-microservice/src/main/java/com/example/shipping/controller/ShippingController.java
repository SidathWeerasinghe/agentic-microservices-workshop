package com.example.shipping.controller;

import com.example.shipping.dto.*;
import com.example.shipping.service.ShippingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Shipping management endpoints")
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/quotes")
    @Operation(summary = "Calculate shipping cost",
            description = "Calculate shipping cost based on items and destination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shipping quote calculated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShippingQuoteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content)
    })
    public ResponseEntity<ShippingQuoteResponse> calculateShippingQuote(
            @Valid @RequestBody ShippingQuoteRequest request) {

        ShippingQuoteResponse response = shippingService.calculateShippingQuote(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirmations")
    @Operation(summary = "Confirm shipping",
            description = "Confirm shipping using a valid quote")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Shipping confirmed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShippingConfirmationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Quote not found",
                    content = @Content),
            @ApiResponse(responseCode = "410", description = "Quote expired",
                    content = @Content)
    })
    public ResponseEntity<ShippingConfirmationResponse> confirmShipping(
            @Valid @RequestBody ShippingConfirmationRequest request) {

        ShippingConfirmationResponse response = shippingService.confirmShipping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
