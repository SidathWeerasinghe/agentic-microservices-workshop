# Orchestrator Microservice - cURL Commands

## Prerequisites

Ensure all microservices are running:
- **Inventory Service**: http://localhost:8080
- **Payment Service**: http://localhost:8081
- **Shipping Service**: http://localhost:8082
- **Orchestrator Service**: http://localhost:8083

The orchestrator runs a fixed order flow: check stock → (production order if low)
→ shipping quote → payment (auto-retry up to 2×) → confirm shipping. It fills each
item's unit price from the stock check, so requests do not send prices.

---

## SCENARIO 1: Successful order (MacBook Neo × 3, Colombo → Colombo)

Grand total 1,050,675 LKR is below the payment step-up threshold, so payment
succeeds on the first attempt.

```bash
curl -X POST "http://localhost:8083/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "branchId": "BRANCH-COL",
    "items": [
        { "itemId": "MACBOOK-NEO-001", "quantity": 3 }
    ],
    "shippingAddress": {
        "line1": "42 Galle Road",
        "city": "Colombo",
        "area": "Colombo 03",
        "country": "Sri Lanka"
    },
    "paymentDetails": { "paymentMethod": "CARD", "currency": "LKR" }
  }'
```

**Expected Response (200) — COMPLETED:**
```json
{
    "orderId": "ORD-...",
    "customerId": "CUST-001",
    "status": "COMPLETED",
    "stockCheckResult": {
        "itemId": "MACBOOK-NEO-001",
        "branchId": "BRANCH-COL",
        "availableQuantity": 20,
        "unitPrice": 350000.00,
        "status": "IN_STOCK",
        "sufficient": true
    },
    "shippingQuoteResult": { "quoteId": "...", "shippingCost": 675.00, "currency": "LKR" },
    "paymentResult": { "paymentReference": "PAY-...", "status": "SUCCESS", "amount": 1050675.00 },
    "shippingConfirmationResult": { "shipmentId": "...", "status": "CONFIRMED" },
    "totalAmount": 1050000.00,
    "shippingCost": 675.00,
    "grandTotal": 1050675.00
}
```

---

## SCENARIO 2: High-value order with automatic payment retry (MacBook Pro × 3)

Grand total 1,800,675 LKR is at or above the step-up threshold (1,500,000). The
payment service holds the first charge; the orchestrator **automatically retries**
and the order completes.

```bash
curl -X POST "http://localhost:8083/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-CORP-01",
    "branchId": "BRANCH-COL",
    "items": [
        { "itemId": "MACBOOK-PRO-001", "quantity": 3 }
    ],
    "shippingAddress": {
        "line1": "88 Reid Avenue",
        "city": "Colombo",
        "area": "Colombo 07",
        "country": "Sri Lanka"
    },
    "paymentDetails": { "paymentMethod": "CARD", "currency": "LKR" }
  }'
```

**Expected Response (200) — COMPLETED after retry:**
```json
{
    "orderId": "ORD-...",
    "status": "COMPLETED",
    "shippingQuoteResult": { "shippingCost": 675.00, "currency": "LKR" },
    "paymentResult": { "paymentReference": "PAY-...", "status": "SUCCESS", "amount": 1800675.00 },
    "shippingConfirmationResult": { "shipmentId": "...", "status": "CONFIRMED" },
    "totalAmount": 1800000.00,
    "shippingCost": 675.00,
    "grandTotal": 1800675.00
}
```

> This is the REST/orchestrator equivalent of the MCP agent's "self-healing
> payment" — but here the retry loop is hand-coded (`MAX_PAYMENT_RETRIES = 2`).

---

## SCENARIO 3: Insufficient stock (MacBook Pro × 15, Colombo has only 10)

```bash
curl -X POST "http://localhost:8083/orders" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-002",
    "branchId": "BRANCH-COL",
    "items": [
        { "itemId": "MACBOOK-PRO-001", "quantity": 15 }
    ],
    "shippingAddress": {
        "line1": "42 Galle Road",
        "city": "Colombo",
        "area": "Colombo 03",
        "country": "Sri Lanka"
    },
    "paymentDetails": { "paymentMethod": "CARD", "currency": "LKR" }
  }'
```

**Expected Response — order rejected at the stock step:**
```json
{
    "orderId": "ORD-...",
    "customerId": "CUST-002",
    "status": "INSUFFICIENT_STOCK",
    "failedStep": "STOCK_CHECK",
    "failureReason": "Insufficient stock: requested 15 but only 10 available at branch BRANCH-COL",
    "stockCheckResult": {
        "itemId": "MACBOOK-PRO-001",
        "branchId": "BRANCH-COL",
        "availableQuantity": 10,
        "status": "IN_STOCK",
        "sufficient": false
    }
}
```

> **Note on branch comparison:** the orchestrator ships from the single branch you
> specify. Comparing branches to find the cheapest *landed* cost (e.g. the iPhone
> Jaffna-vs-Colombo flip) is a decision the fixed flow does not make — that is
> exactly the kind of reasoning the MCP + LLM version adds.

---

## PowerShell Command (Windows)

```powershell
$body = @{
    customerId = "CUST-001"
    branchId = "BRANCH-COL"
    items = @( @{ itemId = "MACBOOK-NEO-001"; quantity = 3 } )
    shippingAddress = @{
        line1 = "42 Galle Road"; city = "Colombo"; area = "Colombo 03"; country = "Sri Lanka"
    }
    paymentDetails = @{ paymentMethod = "CARD"; currency = "LKR" }
} | ConvertTo-Json -Depth 4

Invoke-RestMethod -Uri "http://localhost:8083/orders" -Method Post -Body $body -ContentType "application/json"
```
