# Payment Microservice - cURL Commands

Payment outcomes are simulated (see the README). In the default **deterministic**
demo mode: every charge **succeeds**, except a card charge of **1,500,000 LKR or
more**, which is held for bank step-up authorization on the first attempt and
**clears on retry**.

## Make a Payment (Success)

A normal-value charge succeeds immediately.

```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-NEO-001",
    "amount": 350625.00,
    "currency": "LKR",
    "paymentMethod": "CARD",
    "customerId": "CUST-001"
  }'
```

**Expected Response (201):**
```json
{
    "paymentId": "760f9098-69ac-4301-aba1-cca249070615",
    "paymentReference": "PAY-04629978",
    "orderId": "ORDER-NEO-001",
    "status": "SUCCESS",
    "amount": 350625.00,
    "createdAt": "2026-08-26T10:00:00"
}
```

---

## High-Value Payment — Step-Up Authorization (Fail, then Retry)

A card charge of 1,500,000 LKR or more is **held** on the first attempt.

```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-BULK-001",
    "amount": 1800675.00,
    "currency": "LKR",
    "paymentMethod": "CARD",
    "customerId": "CUST-CORP-01"
  }'
```

**Expected Response (201, held):**
```json
{
    "paymentId": "37cc4384-8e21-4ed9-aec4-01da464520d8",
    "paymentReference": "PAY-C554ECC1",
    "orderId": "ORDER-BULK-001",
    "status": "FAILED",
    "amount": 1800675.00,
    "failureReason": "Bank authorization hold - high-value payment requires re-authorization",
    "createdAt": "2026-08-26T10:00:00"
}
```

Retry with the returned `paymentReference` — it clears:

```bash
curl -X POST "http://localhost:8081/payments/PAY-C554ECC1/retry" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "CARD",
    "reason": "Customer re-authorized with the bank"
  }'
```

**Expected Response (200):**
```json
{
    "paymentId": "37cc4384-8e21-4ed9-aec4-01da464520d8",
    "paymentReference": "PAY-C554ECC1",
    "orderId": "ORDER-BULK-001",
    "status": "SUCCESS",
    "amount": 1800675.00,
    "createdAt": "2026-08-26T10:00:00"
}
```

---

## Payment Methods

### CASH
```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-002",
    "amount": 25000.00,
    "currency": "LKR",
    "paymentMethod": "CASH",
    "customerId": "CUST-002"
  }'
```

### BANK_TRANSFER (customerId optional)
```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORDER-003",
    "amount": 100000.00,
    "currency": "LKR",
    "paymentMethod": "BANK_TRANSFER"
  }'
```

### Validation Error (400) — missing orderId
```bash
curl -X POST "http://localhost:8081/payments" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 1500.00,
    "currency": "LKR",
    "paymentMethod": "CARD"
  }'
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "Invalid request parameters",
    "details": { "orderId": "Order ID is required" }
}
```

---

## Retry Payment Endpoints

### Retry with a Different Method
```bash
curl -X POST "http://localhost:8081/payments/PAY-ABC12345/retry" \
  -H "Content-Type: application/json" \
  -d '{
    "paymentMethod": "BANK_TRANSFER",
    "reason": "Card declined, trying bank transfer"
  }'
```

### Retry with Empty Body
```bash
curl -X POST "http://localhost:8081/payments/PAY-ABC12345/retry" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Retry Non-existent Payment (404)
```bash
curl -X POST "http://localhost:8081/payments/PAY-INVALID123/retry" \
  -H "Content-Type: application/json" \
  -d '{}'
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Payment not found with reference: PAY-INVALID123"
}
```

> Note: retrying a payment that already succeeded returns **422 Unprocessable Entity**
> ("Cannot retry a successful payment").

---

## Get Payment Details

```bash
curl -X GET "http://localhost:8081/payments/PAY-ABC12345"
```

**Expected Response (200):**
```json
{
    "paymentId": "550e8400-e29b-41d4-a716-446655440000",
    "paymentReference": "PAY-ABC12345",
    "orderId": "ORDER-NEO-001",
    "status": "SUCCESS",
    "amount": 350625.00,
    "createdAt": "2026-08-26T10:00:00"
}
```

---

## Payment Methods

| Method | Description |
|--------|-------------|
| CARD | Credit/Debit card payment |
| CASH | Cash payment |
| BANK_TRANSFER | Bank transfer payment |

## Payment Statuses

| Status | Description |
|--------|-------------|
| PENDING | Payment is being processed |
| SUCCESS | Payment completed successfully |
| FAILED | Payment failed (check failureReason) |

---

## PowerShell Commands (Windows)

### Make Payment
```powershell
$body = @{
    orderId = "ORDER-NEO-001"
    amount = 350625.00
    currency = "LKR"
    paymentMethod = "CARD"
    customerId = "CUST-001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/payments" -Method Post -Body $body -ContentType "application/json"
```

### Retry Payment
```powershell
$body = @{
    paymentMethod = "CARD"
    reason = "Customer re-authorized with the bank"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/payments/PAY-C554ECC1/retry" -Method Post -Body $body -ContentType "application/json"
```

### Get Payment
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/payments/PAY-ABC12345" -Method Get
```
