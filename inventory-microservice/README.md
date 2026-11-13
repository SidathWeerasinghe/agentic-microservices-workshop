# Inventory Microservice

A Spring Boot microservice that handles stock checks and production orders for branches.

## Features

- **Stock Management**: Check stock availability for items at specific branches
- **Production Orders**: Create production orders for items at branches
- **OpenAPI Documentation**: Auto-generated Swagger UI for API exploration
- **H2 Database**: In-memory database for development with sample data

## API Endpoints

### Stock

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/stock?itemId={itemId}&branchId={branchId}` | Check stock for an item at a branch |

### Production Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/production-orders` | Create a new production order |

## Tech Stack

- **Java 21**
- **Spring Boot 3.5.11**
- **Spring Data JPA**
- **H2 Database** (Development)
- **PostgreSQL** (Production)
- **Lombok**
- **SpringDoc OpenAPI**

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.6+

### Running the Application

```bash
cd inventory-microservice

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### API Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs
- **H2 Console**: http://localhost:8080/h2-console (Username: `sa`, Password: empty)

## Sample Data

The application initializes with sample stock data across four Sri Lankan
branches — Colombo (`BRANCH-COL`), Gampaha (`BRANCH-GAM`), Jaffna (`BRANCH-JAF`),
and Kandy (`BRANCH-KDY`):

| Item ID | Product | Branch | Qty | Unit Price (LKR) | Status |
|---------|---------|--------|-----|------------------|--------|
| MACBOOK-PRO-001 | Apple MacBook Pro 14" M3 | Colombo | 10 | 600,000 | IN_STOCK |
| MACBOOK-AIR-001 | Apple MacBook Air 13" M2 | Colombo | 0 | 450,000 | OUT_OF_STOCK |
| MACBOOK-AIR-001 | Apple MacBook Air 13" M2 | Gampaha | 15 | 450,000 | IN_STOCK |
| MACBOOK-NEO-001 | Apple MacBook Neo 12" | Colombo | 20 | 350,000 | IN_STOCK |
| GALAXY-S24-001 | Samsung Galaxy S24 Ultra 256GB | Colombo | 25 | 360,000 | IN_STOCK |
| GALAXY-S24-001 | Samsung Galaxy S24 Ultra 256GB | Jaffna | 30 | 320,000 | IN_STOCK |
| DELL-XPS-001 | Dell XPS 15 Laptop Intel i7 | Gampaha | 15 | 280,000 | IN_STOCK |
| DELL-XPS-001 | Dell XPS 15 Laptop Intel i7 | Kandy | 20 | 310,000 | IN_STOCK |
| IPHONE-15-001 | Apple iPhone 15 Pro 256GB | Jaffna | 40 | 340,000 | IN_STOCK |
| IPHONE-15-001 | Apple iPhone 15 Pro 256GB | Colombo | 35 | 350,000 | IN_STOCK |

## Example Requests

### Check Stock

```bash
curl "http://localhost:8080/stock?itemId=MACBOOK-PRO-001&branchId=BRANCH-COL"
```

Response:
```json
{
  "itemId": "MACBOOK-PRO-001",
  "branchId": "BRANCH-COL",
  "availableQuantity": 10,
  "unitPrice": 600000.00,
  "status": "IN_STOCK"
}
```

### Create Production Order

```bash
curl -X POST "http://localhost:8080/production-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "MACBOOK-NEO-001",
    "branchId": "BRANCH-COL",
    "requestedQuantity": 5,
    "reason": "Order ORD-1001 - allocate stock"
  }'
```

Response:
```json
{
  "productionOrderId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "CREATED"
}
```

## Running Tests

```bash
mvn test
```

## Production Configuration

For production deployment, use the `prod` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Set the following environment variables:
- `DB_USERNAME`: PostgreSQL username
- `DB_PASSWORD`: PostgreSQL password

## Project Structure

```
inventory-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/example/inventory/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enum types
│   │   │   ├── exception/        # Exception handling
│   │   │   ├── repository/       # Data repositories
│   │   │   ├── service/          # Business logic
│   │   │   └── InventoryMicroserviceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/                     # Unit tests
└── pom.xml
```
