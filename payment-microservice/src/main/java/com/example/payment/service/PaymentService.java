package com.example.payment.service;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentRetryRequest;
import com.example.payment.entity.Payment;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.PaymentNotFoundException;
import com.example.payment.exception.PaymentProcessingException;
import com.example.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Random random = new Random();

    // Demo mode: when true, outcomes are deterministic so live demos never surprise you.
    // Set payment.simulation.deterministic=false to restore realistic 70/30 randomness.
    @Value("${payment.simulation.deterministic:true}")
    private boolean deterministic;

    // Step-up authorization: card charges at or above this amount (LKR) get a bank
    // authorization hold on the FIRST attempt and clear on retry — real high-value
    // payment behaviour, triggered purely by the order total (no IDs, no magic names).
    @Value("${payment.simulation.step-up-threshold:1500000}")
    private BigDecimal stepUpThreshold;

    @Transactional
    public PaymentResponse makePayment(PaymentRequest request) {
        log.info("Processing payment for orderId: {}, amount: {} {}", 
                request.getOrderId(), request.getAmount(), request.getCurrency());

        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        PaymentStatus finalStatus = decideInitialOutcome(request.getAmount());
        payment.setStatus(finalStatus);

        if (finalStatus == PaymentStatus.FAILED) {
            payment.setFailureReason(isHighValue(request.getAmount())
                    ? "Bank authorization hold - high-value payment requires re-authorization"
                    : getRandomFailureReason());
        }

        payment = paymentRepository.save(payment);

        log.info("Payment {} completed with status: {}", payment.getPaymentReference(), finalStatus);

        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse retryPayment(String paymentReference, PaymentRetryRequest request) {
        log.info("Retrying payment with reference: {}", paymentReference);

        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with reference: " + paymentReference));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentProcessingException("Cannot retry a successful payment");
        }

        if (request != null && request.getPaymentMethod() != null) {
            payment.setPaymentMethod(request.getPaymentMethod());
        }

        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setFailureReason(null);

        // In deterministic (demo) mode a retry always succeeds, so a FAIL customer's
        // payment self-heals in exactly one retry. Otherwise fall back to 70/30.
        PaymentStatus finalStatus = deterministic ? PaymentStatus.SUCCESS : simulatePaymentProcessing();
        payment.setStatus(finalStatus);

        if (finalStatus == PaymentStatus.FAILED) {
            payment.setFailureReason(getRandomFailureReason());
        }

        payment = paymentRepository.save(payment);

        log.info("Payment retry {} completed with status: {} (attempt #{})", 
                payment.getPaymentReference(), finalStatus, payment.getRetryCount());

        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found with reference: " + paymentReference));
        return mapToResponse(payment);
    }

    private PaymentStatus simulatePaymentProcessing() {
        int outcome = random.nextInt(100);
        if (outcome < 70) {
            return PaymentStatus.SUCCESS;
        } else {
            return PaymentStatus.FAILED;
        }
    }

    /** First-attempt outcome: high-value charges are held for step-up auth; the rest clear. */
    private PaymentStatus decideInitialOutcome(BigDecimal amount) {
        if (!deterministic) {
            return simulatePaymentProcessing(); // realistic 70/30
        }
        return isHighValue(amount) ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
    }

    private boolean isHighValue(BigDecimal amount) {
        return amount != null && stepUpThreshold != null
                && amount.compareTo(stepUpThreshold) >= 0;
    }

    private String getRandomFailureReason() {
        String[] reasons = {
                "Insufficient funds",
                "Card declined",
                "Payment gateway timeout",
                "Invalid card details",
                "Bank authorization failed"
        };
        return reasons[random.nextInt(reasons.length)];
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentReference(payment.getPaymentReference())
                .orderId(payment.getOrderId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .build();
    }

}
