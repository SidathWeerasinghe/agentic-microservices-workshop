package com.example.inventory.config;

import com.example.inventory.entity.Stock;
import com.example.inventory.enums.StockStatus;
import com.example.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class DataInitializer implements CommandLineRunner {

    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample stock data...");

        // MacBook Order Scenarios - Sri Lanka Branches
        // Scenario 1: MacBook Pro at Colombo - AVAILABLE (IN_STOCK)
        createStock("MACBOOK-PRO-001", "Apple MacBook Pro 14-inch M3", "BRANCH-COL", "Colombo", 10, 600000.0);
        
        // Scenario 2: MacBook Air at Colombo - NOT AVAILABLE (OUT_OF_STOCK)
        createStock("MACBOOK-AIR-001", "Apple MacBook Air 13-inch M2", "BRANCH-COL", "Colombo", 0, 450000.0);
        
        // Scenario 3: MacBook Air at Gampaha - AVAILABLE (IN_STOCK)
        createStock("MACBOOK-AIR-001", "Apple MacBook Air 13-inch M2", "BRANCH-GAM", "Gampaha", 15, 450000.0);
        
        // Scenario 4: MacBook Neo at Colombo - AVAILABLE (IN_STOCK)
        createStock("MACBOOK-NEO-001", "Apple MacBook Neo 12-inch", "BRANCH-COL", "Colombo", 20, 350000.0);

        // Multi-Branch Price Comparison Scenarios - Samsung Galaxy S24
        // Scenario 5A: Galaxy S24 at Colombo - Higher price (360,000), Lower shipping (3,000) = Total 363,000
        createStock("GALAXY-S24-001", "Samsung Galaxy S24 Ultra 256GB", "BRANCH-COL", "Colombo", 25, 360000.0);
        
        // Scenario 5B: Galaxy S24 at Jaffna - Lower price (320,000), Higher shipping (15,000) = Total 335,000
        createStock("GALAXY-S24-001", "Samsung Galaxy S24 Ultra 256GB", "BRANCH-JAF", "Jaffna", 30, 320000.0);

        // Multi-Branch Price Comparison Scenarios - Dell XPS Laptop
        // Scenario 6A: Dell XPS at Gampaha - Lower price (280,000), Higher shipping (12,000) = Total 292,000
        createStock("DELL-XPS-001", "Dell XPS 15 Laptop Intel i7", "BRANCH-GAM", "Gampaha", 15, 280000.0);

        // Scenario 6B: Dell XPS at Kandy - Higher price (310,000), Lower shipping (4,000) = Total 314,000
        createStock("DELL-XPS-001", "Dell XPS 15 Laptop Intel i7", "BRANCH-KDY", "Kandy", 20, 310000.0);

        // "Landed cost flip" scenario - iPhone 15 Pro (small price gap, big distance gap)
        // Jaffna is CHEAPER on sticker (340,000) but ~400 km from Colombo -> +20,000 shipping.
        // Colombo is pricier on sticker (350,000) but LOCAL -> +500 shipping.
        // Delivered to Colombo: Jaffna 360,125 vs Colombo 350,625 -> Colombo wins despite higher price.
        createStock("IPHONE-15-001", "Apple iPhone 15 Pro 256GB", "BRANCH-JAF", "Jaffna", 40, 340000.0);
        createStock("IPHONE-15-001", "Apple iPhone 15 Pro 256GB", "BRANCH-COL", "Colombo", 35, 350000.0);

        log.info("Sample data initialization completed. Total stocks: {}", stockRepository.count());
    }

    private void createStock(String itemId, String itemName, String branchId, String branchName, int quantity, double unitPrice) {
        Stock stock = Stock.builder()
                .itemId(itemId)
                .itemName(itemName)
                .branchId(branchId)
                .branchName(branchName)
                .availableQuantity(quantity)
                .unitPrice(BigDecimal.valueOf(unitPrice))
                .status(determineStatus(quantity))
                .build();
        stockRepository.save(stock);
        log.debug("Created stock: {} ({}) at {} with quantity {} and price {}", itemId, itemName, branchId, quantity, unitPrice);
    }

    private StockStatus determineStatus(int quantity) {
        if (quantity <= 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (quantity < 10) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

}
