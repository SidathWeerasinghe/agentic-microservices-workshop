package com.example.inventory.repository;

import com.example.inventory.entity.ProductionOrder;
import com.example.inventory.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, String> {

    List<ProductionOrder> findByItemIdAndBranchId(String itemId, String branchId);

    List<ProductionOrder> findByStatus(OrderStatus status);

    List<ProductionOrder> findByBranchId(String branchId);

}
