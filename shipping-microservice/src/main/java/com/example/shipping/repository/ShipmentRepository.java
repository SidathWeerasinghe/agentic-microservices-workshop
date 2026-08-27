package com.example.shipping.repository;

import com.example.shipping.entity.Shipment;
import com.example.shipping.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {

    Optional<Shipment> findByOrderId(String orderId);

    List<Shipment> findByCustomerId(String customerId);

    List<Shipment> findByStatus(ShipmentStatus status);

    List<Shipment> findByBranchId(String branchId);

}
