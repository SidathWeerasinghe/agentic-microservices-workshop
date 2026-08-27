package com.example.payment.repository;

import com.example.payment.entity.Payment;
import com.example.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByCustomerId(String customerId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByPaymentReference(String paymentReference);

}
