# Shipping Microservice

A Spring Boot microservice that handles shipping cost calculations and confirmations.

## Features

- **Shipping Quotes**: Calculate shipping costs based on items, weight, and destination
- **Shipping Confirmation**: Confirm shipping using valid quotes
- **Domestic vs International**: Different rates for domestic and international shipping
- **Quote Expiry**: Quotes expire after 24 hours
- **OpenAPI Documentation**: Auto-generated Swagger UI for API exploration

## API Endpoints

### Shipping

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/quotes` | Calculate shipping cost |
| POST | `/confirmations` | Confirm shipping with a quote |

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
cd shipping-microservice

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8082`

### API Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8082/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8082/api-docs
- **H2 Console**: http://localhost:8082/h2-console (Username: `sa`, Password: empty)

## Shipping Cost Calculation

Shipping cost depends on weight, item count, and the **distance** from the origin
branch's city to the destination city.

| Component | Rate |
|-----------|------|
| Base Rate | 100.00 LKR |
| Per KG | 50.00 LKR |
| Per Item | 25.00 LKR |
| International Multiplier | 2.5x |

**Distance zone surcharge** (origin branch city → destination city):

| Distance | Surcharge |
|----------|-----------|
| Same city (local) | 500 LKR |
| ≤ 50 km | 1,500 LKR |
| ≤ 150 km | 5,000 LKR |
| ≤ 300 km | 12,000 LKR |
| > 300 km | 20,000 LKR |

Branch cities: Colombo, Gampaha, Kandy, Jaffna. Approximate distances (km):
Colombo–Gampaha 30, Colombo–Kandy 115, Colombo–Jaffna 400, Gampaha–Kandy 90,
Gampaha–Jaffna 380, Kandy–Jaffna 290. An unknown domestic city defaults to 150 km.

**Formula:**
```
Domestic:      Base + (Weight × 50) + (Items × 25) + DistanceSurcharge
International:  [ Base + (Weight × 50) + (Items × 25) + DistanceSurcharge ] × 2.5
```

## Example Requests

### Calculate Shipping Quote

```bash
curl -X POST "http://localhost:8082/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-COL",
    "destinationAddress": {
        "line1": "123 Galle Road",
        "city": "Colombo",
        "area": "Colombo 03",
        "country": "Sri Lanka"
    },
    "items": [
        {
            "itemId": "MACBOOK-PRO-001",
            "quantity": 2,
            "weightKg": 1.5
        }
    ]
  }'
```

Response (2 items, 3.0 kg, local Colombo delivery: 100 + 150 + 50 + 500):
```json
{
    "quoteId": "550e8400-e29b-41d4-a716-446655440000",
    "shippingCost": 800.00,
    "currency": "LKR"
}
```

### Confirm Shipping

```bash
curl -X POST "http://localhost:8082/confirmations" \
  -H "Content-Type: application/json" \
  -d '{
    "quoteId": "550e8400-e29b-41d4-a716-446655440000",
    "orderId": "ORDER-001",
    "customerId": "CUST-001"
  }'
```

Response:
```json
{
    "shipmentId": "660e8400-e29b-41d4-a716-446655440000",
    "status": "CONFIRMED"
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
shipping-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/example/shipping/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enum types
│   │   │   ├── exception/        # Exception handling
│   │   │   ├── repository/       # Data repositories
│   │   │   ├── service/          # Business logic
│   │   │   └── ShippingMicroserviceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/                     # Unit tests
├── sample-data/                  # Sample API requests
└── pom.xml
```
