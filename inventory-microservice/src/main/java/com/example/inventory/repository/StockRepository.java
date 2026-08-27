package com.example.inventory.repository;

import com.example.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByItemIdAndBranchId(String itemId, String branchId);

    boolean existsByItemIdAndBranchId(String itemId, String branchId);

    List<Stock> findByItemIdContainingIgnoreCaseOrItemNameContainingIgnoreCase(String itemId, String itemName);

    @Query("SELECT DISTINCT s.itemId, s.itemName FROM Stock s")
    List<Object[]> findDistinctItems();

}
