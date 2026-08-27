package com.example.inventory.service;

import com.example.inventory.dto.ProductionOrderRequest;
import com.example.inventory.dto.ProductionOrderResponse;
import com.example.inventory.entity.ProductionOrder;
import com.example.inventory.entity.Stock;
import com.example.inventory.enums.OrderStatus;
import com.example.inventory.exception.InsufficientStockException;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.repository.ProductionOrderRepository;
import com.example.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final StockRepository stockRepository;

    @Transactional
    public ProductionOrderResponse createProductionOrder(ProductionOrderRequest request) {
        log.info("Creating production order for itemId: {} at branchId: {} with quantity: {}",
                request.getItemId(), request.getBranchId(), request.getRequestedQuantity());

        Stock stock = stockRepository.findByItemIdAndBranchId(request.getItemId(), request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Stock not found for item '%s' at branch '%s'",
                                request.getItemId(), request.getBranchId())));

        if (request.getRequestedQuantity() > stock.getAvailableQuantity()) {
            String message = String.format(
                    "Requested quantity %d exceeds available stock %d for item '%s' at branch '%s'",
                    request.getRequestedQuantity(), stock.getAvailableQuantity(),
                    request.getItemId(), request.getBranchId());
            log.warn("Production order rejected - {}", message);
            throw new InsufficientStockException(message);
        }

        int remainingQuantity = stock.getAvailableQuantity() - request.getRequestedQuantity();
        stock.setAvailableQuantity(remainingQuantity);
        stockRepository.save(stock);
        log.info("Reduced stock for item '{}' at branch '{}' to {}",
                request.getItemId(), request.getBranchId(), remainingQuantity);

        ProductionOrder productionOrder = ProductionOrder.builder()
                .itemId(request.getItemId())
                .branchId(request.getBranchId())
                .requestedQuantity(request.getRequestedQuantity())
                .reason(request.getReason())
                .status(OrderStatus.CREATED)
                .build();

        ProductionOrder savedOrder = productionOrderRepository.save(productionOrder);

        log.info("Production order created with ID: {}", savedOrder.getProductionOrderId());

        return ProductionOrderResponse.builder()
                .productionOrderId(savedOrder.getProductionOrderId())
                .status(savedOrder.getStatus())
                .build();
    }

    @Transactional
    public ProductionOrder updateOrderStatus(String productionOrderId, OrderStatus newStatus) {
        ProductionOrder order = productionOrderRepository.findById(productionOrderId)
                .orElseThrow(() -> new RuntimeException("Production order not found: " + productionOrderId));
        
        order.setStatus(newStatus);
        return productionOrderRepository.save(order);
    }

}
