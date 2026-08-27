package com.example.orchestrator.client;

import com.example.orchestrator.dto.PaymentResult;
import com.example.orchestrator.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class PaymentServiceClient {

    private final WebClient webClient;

    public PaymentServiceClient(@Qualifier("paymentWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public PaymentResult makePayment(String orderId, BigDecimal amount, String currency, 
                                      String paymentMethod, String customerId) {
        log.info("Making payment for order: {}, amount: {} {}", orderId, amount, currency);

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("orderId", orderId);
            request.put("amount", amount);
            request.put("currency", currency != null ? currency : "LKR");
            request.put("paymentMethod", paymentMethod);
            request.put("customerId", customerId);

            Map<String, Object> response = webClient.post()
                    .uri("/payments")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Payment validation failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Payment service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                BigDecimal responseAmount = response.get("amount") instanceof Number ?
                        new BigDecimal(response.get("amount").toString()) : BigDecimal.ZERO;

                return PaymentResult.builder()
                        .paymentId((String) response.get("paymentId"))
                        .paymentReference((String) response.get("paymentReference"))
                        .status((String) response.get("status"))
                        .amount(responseAmount)
                        .failureReason((String) response.get("failureReason"))
                        .build();
            }
            throw new ServiceException("Empty response from payment service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error making payment: {}", e.getMessage());
            throw new ServiceException("Failed to make payment: " + e.getMessage());
        }
    }

    public PaymentResult retryPayment(String paymentReference, String newPaymentMethod) {
        log.info("Retrying payment: {}", paymentReference);

        try {
            Map<String, Object> request = new HashMap<>();
            if (newPaymentMethod != null) {
                request.put("paymentMethod", newPaymentMethod);
            }
            request.put("reason", "Automatic retry from orchestrator");

            Map<String, Object> response = webClient.post()
                    .uri("/payments/{paymentReference}/retry", paymentReference)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new ServiceException("Payment retry failed")))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new ServiceException("Payment service unavailable")))
                    .bodyToMono(Map.class)
                    .block();

            if (response != null) {
                BigDecimal responseAmount = response.get("amount") instanceof Number ?
                        new BigDecimal(response.get("amount").toString()) : BigDecimal.ZERO;

                return PaymentResult.builder()
                        .paymentId((String) response.get("paymentId"))
                        .paymentReference((String) response.get("paymentReference"))
                        .status((String) response.get("status"))
                        .amount(responseAmount)
                        .failureReason((String) response.get("failureReason"))
                        .build();
            }
            throw new ServiceException("Empty response from payment service");
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrying payment: {}", e.getMessage());
            throw new ServiceException("Failed to retry payment: " + e.getMessage());
        }
    }

}
