package com.example.payment.dto;

import com.example.payment.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRetryRequest {

    private PaymentMethod paymentMethod;
    
    private String reason;

}
