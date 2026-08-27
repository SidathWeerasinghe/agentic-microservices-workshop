package com.example.inventory.service;

import com.example.inventory.dto.ItemCatalogResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.entity.Stock;
import com.example.inventory.exception.ResourceNotFoundException;
import com.example.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public StockResponse checkStock(String itemId, String branchId) {
        log.info("Checking stock for itemId: {} at branchId: {}", itemId, branchId);
        
        Stock stock = stockRepository.findByItemIdAndBranchId(itemId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Stock not found for item '%s' at branch '%s'", itemId, branchId)));

        return StockResponse.builder()
                .itemId(stock.getItemId())
                .branchId(stock.getBranchId())
                .availableQuantity(stock.getAvailableQuantity())
                .unitPrice(stock.getUnitPrice())
                .status(stock.getStatus())
                .build();
    }

    @Transactional
    public Stock updateStock(String itemId, String branchId, int quantity) {
        Stock stock = stockRepository.findByItemIdAndBranchId(itemId, branchId)
                .orElse(Stock.builder()
                        .itemId(itemId)
                        .branchId(branchId)
                        .availableQuantity(0)
                        .build());
        
        stock.setAvailableQuantity(quantity);
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock addStock(String itemId, String branchId, int quantityToAdd) {
        Stock stock = stockRepository.findByItemIdAndBranchId(itemId, branchId)
                .orElse(Stock.builder()
                        .itemId(itemId)
                        .branchId(branchId)
                        .availableQuantity(0)
                        .build());
        
        stock.setAvailableQuantity(stock.getAvailableQuantity() + quantityToAdd);
        return stockRepository.save(stock);
    }

    @Transactional
    public Stock reduceStock(String itemId, String branchId, int quantityToReduce) {
        Stock stock = stockRepository.findByItemIdAndBranchId(itemId, branchId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Stock not found for item '%s' at branch '%s'", itemId, branchId)));
        
        int newQuantity = Math.max(0, stock.getAvailableQuantity() - quantityToReduce);
        stock.setAvailableQuantity(newQuantity);
        return stockRepository.save(stock);
    }

    @Transactional(readOnly = true)
    public List<ItemCatalogResponse> getAllItemsCatalog() {
        log.info("Fetching all items catalog");
        
        List<Stock> allStocks = stockRepository.findAll();
        
        Map<String, List<Stock>> groupedByItemId = allStocks.stream()
                .collect(Collectors.groupingBy(Stock::getItemId));
        
        return groupedByItemId.entrySet().stream()
                .map(entry -> {
                    String itemId = entry.getKey();
                    List<Stock> stocks = entry.getValue();
                    String itemName = stocks.get(0).getItemName();
                    
                    List<ItemCatalogResponse.BranchStock> branches = stocks.stream()
                            .map(stock -> ItemCatalogResponse.BranchStock.builder()
                                    .branchId(stock.getBranchId())
                                    .branchName(stock.getBranchName())
                                    .availableQuantity(stock.getAvailableQuantity())
                                    .unitPrice(stock.getUnitPrice())
                                    .status(stock.getStatus())
                                    .build())
                            .collect(Collectors.toList());
                    
                    return ItemCatalogResponse.builder()
                            .itemId(itemId)
                            .itemName(itemName)
                            .branches(branches)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ItemCatalogResponse> searchItems(String searchTerm) {
        log.info("Searching items with term: {}", searchTerm);
        
        List<Stock> matchingStocks = stockRepository
                .findByItemIdContainingIgnoreCaseOrItemNameContainingIgnoreCase(searchTerm, searchTerm);
        
        Map<String, List<Stock>> groupedByItemId = matchingStocks.stream()
                .collect(Collectors.groupingBy(Stock::getItemId));
        
        return groupedByItemId.entrySet().stream()
                .map(entry -> {
                    String itemId = entry.getKey();
                    List<Stock> stocks = entry.getValue();
                    String itemName = stocks.get(0).getItemName();
                    
                    List<ItemCatalogResponse.BranchStock> branches = stocks.stream()
                            .map(stock -> ItemCatalogResponse.BranchStock.builder()
                                    .branchId(stock.getBranchId())
                                    .branchName(stock.getBranchName())
                                    .availableQuantity(stock.getAvailableQuantity())
                                    .unitPrice(stock.getUnitPrice())
                                    .status(stock.getStatus())
                                    .build())
                            .collect(Collectors.toList());
                    
                    return ItemCatalogResponse.builder()
                            .itemId(itemId)
                            .itemName(itemName)
                            .branches(branches)
                            .build();
                })
                .collect(Collectors.toList());
    }

}
