package com.example.payment.dto;

import com.example.payment.enums.PaymentStatus;
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
public class PaymentResponse {

    private String paymentId;
    private String paymentReference;
    private String orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime createdAt;

}
