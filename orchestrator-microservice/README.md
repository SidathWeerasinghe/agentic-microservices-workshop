# Order Orchestrator Microservice

A Spring Boot microservice that orchestrates the complete order flow across Inventory, Payment, and Shipping microservices.

## Features

- **Complete Order Flow**: Coordinates stock check, production order, shipping quote, payment, and shipping confirmation
- **Automatic Payment Retry**: Retries failed payments up to 2 times
- **Partial Response on Failure**: Returns completed steps even when orchestration fails
- **Service Health Monitoring**: Tracks availability of downstream services

## Architecture

```
                         ┌─────────────────────┐
                         │   Orchestrator      │
                         │   (Port 8083)       │
                         └─────────┬───────────┘
                                   │
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│  Inventory Service  │  │  Payment Service    │  │  Shipping Service   │
│   (Port 8080)       │  │   (Port 8081)       │  │   (Port 8082)       │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
```

## Order Flow

```
1. Check Stock (Inventory)
        │
        ▼
2. Create Production Order (if low stock)
        │
        ▼
3. Calculate Shipping Quote (Shipping)
        │
        ▼
4. Process Payment (Payment)
        │
        ▼
5. Confirm Shipping (Shipping)
        │
        ▼
   Order Complete!
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/orders` | Process a complete order |

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.11**
- **Spring WebFlux** (WebClient for service calls)
- **Lombok**
- **SpringDoc OpenAPI**

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+
- All downstream services running:
  - Inventory Service (port 8080)
  - Payment Service (port 8081)
  - Shipping Service (port 8082)

### Running All Services

Start services in this order:

```bash
# Terminal 1 - Inventory Service
cd inventory-microservice
mvn spring-boot:run

# Terminal 2 - Payment Service
cd payment-microservice
mvn spring-boot:run

# Terminal 3 - Shipping Service
cd shipping-microservice
mvn spring-boot:run

# Terminal 4 - Orchestrator Service
cd orchestrator-microservice
mvn spring-boot:run
```

### API Documentation

- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8083/api-docs

## Example Request

Order 3 MacBook Neo from the Colombo branch, delivered within Colombo. The
orchestrator fills the unit price from the stock check, so the request does not
need to send prices.

```bash
curl -X POST "http://localhost:8083/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "branchId": "BRANCH-COL",
    "items": [
        {
            "itemId": "MACBOOK-NEO-001",
            "quantity": 3
        }
    ],
    "shippingAddress": {
        "line1": "42 Galle Road",
        "city": "Colombo",
        "area": "Colombo 03",
        "country": "Sri Lanka"
    },
    "paymentDetails": {
        "paymentMethod": "CARD",
        "currency": "LKR"
    }
  }'
```

## Example Response

```json
{
    "orderId": "ORD-ABC12345",
    "customerId": "CUST-001",
    "status": "COMPLETED",
    "stockCheckResult": {
        "itemId": "MACBOOK-NEO-001",
        "availableQuantity": 20,
        "unitPrice": 350000.00,
        "status": "IN_STOCK"
    },
    "shippingQuoteResult": {
        "quoteId": "...",
        "shippingCost": 675.00,
        "currency": "LKR"
    },
    "paymentResult": {
        "paymentReference": "PAY-XYZ789",
        "status": "SUCCESS",
        "amount": 1050675.00
    },
    "shippingConfirmationResult": {
        "shipmentId": "...",
        "status": "CONFIRMED"
    },
    "totalAmount": 1050000.00,
    "shippingCost": 675.00,
    "grandTotal": 1050675.00
}
```

> **High-value orders:** if the grand total reaches 1,500,000 LKR (e.g. 3 MacBook
> Pro at 600,000 each), the payment service holds the first charge for step-up
> authorization. The orchestrator automatically retries (up to 2 times), so the
> order still completes — mirroring what the AI agent does in the MCP version.

## Configuration

```properties
# Service URLs (application.properties)
services.inventory.url=http://localhost:8080
services.payment.url=http://localhost:8081
services.shipping.url=http://localhost:8082
```

## Error Handling

- **Service Unavailable (503)**: When downstream service is unreachable
- **Orchestration Failed (422)**: When a step in the flow fails, includes partial result

## Project Structure

```
orchestrator-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/example/orchestrator/
│   │   │   ├── client/           # Service clients
│   │   │   ├── config/           # Configuration
│   │   │   ├── controller/       # REST controller
│   │   │   ├── dto/              # Request/Response DTOs
│   │   │   ├── exception/        # Exception handling
│   │   │   ├── service/          # Orchestration logic
│   │   │   └── OrchestratorMicroserviceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── sample-data/
└── pom.xml
```
