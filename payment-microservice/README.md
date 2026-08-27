# Payment Microservice

A Spring Boot microservice that handles payments and retries.

## Features

- **Make Payments**: Process payments with various methods (CARD, CASH, BANK_TRANSFER)
- **Retry Payments**: Retry failed payments with optional method change
- **Payment Status Tracking**: Track payment status (PENDING, SUCCESS, FAILED)
- **OpenAPI Documentation**: Auto-generated Swagger UI for API exploration

## API Endpoints

### Payments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/payments` | Make a new payment |
| POST | `/payments/{paymentReference}/retry` | Retry a failed payment |
| GET | `/payments/{paymentReference}` | Get payment details |

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
cd payment-microservice

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8081`

### API Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8081/api-docs
- **H2 Console**: http://localhost:8081/h2-console (Username: `sa`, Password: empty)

## Payment Methods

| Method | Description |
|--------|-------------|
| `CARD` | Credit/Debit card payment |
| `CASH` | Cash payment |
| `BANK_TRANSFER` | Bank transfer payment |

## Payment Statuses

| Status | Description |
|--------|-------------|
| `PENDING` | Payment is being processed |
| `SUCCESS` | Payment completed successfully |
| `FAILED` | Payment failed (check failureReason) |

## Example Requests

### Make a Payment

```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-001",
    "amount": 1500.00,
    "currency": "LKR",
    "paymentMethod": "CARD",
    "customerId": "CUST-001"
  }'
```

Response:
```json
{
    "paymentId": "550e8400-e29b-41d4-a716-446655440000",
    "paymentReference": "PAY-ABC12345",
    "orderId": "ORDER-001",
    "status": "SUCCESS",
    "amount": 1500.00,
    "createdAt": "2026-03-07T10:00:00"
}
```

### Retry a Payment

```bash
curl -X POST "http://localhost:8081/payments/PAY-ABC12345/retry" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "BANK_TRANSFER",
    "reason": "Card declined, trying bank transfer"
  }'
```

### Get Payment Details

```bash
curl -X GET "http://localhost:8081/payments/PAY-ABC12345"
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
payment-microservice/
├── src/
│   ├── main/
│   │   ├── java/com/example/payment/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── enums/            # Enum types
│   │   │   ├── exception/        # Exception handling
│   │   │   ├── repository/       # Data repositories
│   │   │   ├── service/          # Business logic
│   │   │   └── PaymentMicroserviceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/                     # Unit tests
├── sample-data/                  # Sample API requests
└── pom.xml
```

## Payment Simulation Behaviour

Outcomes are simulated and controlled by two properties in `application.properties`:

- `payment.simulation.deterministic` (default `true`) — makes outcomes predictable
  for demos. When `true`, every charge succeeds **except** high-value ones (below).
  Set to `false` for a realistic 70/30 random success rate.
- `payment.simulation.step-up-threshold` (default `1500000` LKR) — card charges at
  or above this amount are held for bank **step-up authorization** on the first
  attempt (status `FAILED`, `failureReason` = "Bank authorization hold …") and then
  **clear on retry**. This models real high-value-transaction behaviour.

Other notes:

- Payment references are auto-generated in the format `PAY-XXXXXXXX`.
- Failed payments include a `failureReason` field with details.
- A successful payment cannot be retried (returns 422).
