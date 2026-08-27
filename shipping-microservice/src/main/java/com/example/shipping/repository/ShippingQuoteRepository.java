package com.example.shipping.repository;

import com.example.shipping.entity.ShippingQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShippingQuoteRepository extends JpaRepository<ShippingQuote, String> {

    Optional<ShippingQuote> findByQuoteIdAndIsUsedFalse(String quoteId);

    List<ShippingQuote> findByBranchId(String branchId);

    List<ShippingQuote> findByExpiresAtBeforeAndIsUsedFalse(LocalDateTime dateTime);

}
