package com.example.payment.controller;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentRetryRequest;
import com.example.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Make a payment",
            description = "Process a new payment for an order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponse> makePayment(
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.makePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentReference}/retry")
    @Operation(summary = "Retry payment",
            description = "Retry a failed payment using the payment reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment retried successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content),
            @ApiResponse(responseCode = "422", description = "Cannot retry payment",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponse> retryPayment(
            @Parameter(description = "Payment reference", required = true)
            @PathVariable String paymentReference,
            @RequestBody(required = false) PaymentRetryRequest request) {

        PaymentResponse response = paymentService.retryPayment(paymentReference, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentReference}")
    @Operation(summary = "Get payment details",
            description = "Retrieve payment details by payment reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment details retrieved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponse> getPayment(
            @Parameter(description = "Payment reference", required = true)
            @PathVariable String paymentReference) {

        PaymentResponse response = paymentService.getPayment(paymentReference);
        return ResponseEntity.ok(response);
    }

}
