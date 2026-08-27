# Shipping Microservice - cURL Commands

Shipping cost = `Base 100 + 50/kg + 25/item + distance-zone surcharge`, and the
whole cost is multiplied by **2.5** for international destinations. The distance
surcharge depends on the origin branch's city and the destination city (see the
README for the zone table and inter-city distances).

Branches: Colombo (`BRANCH-COL`), Gampaha (`BRANCH-GAM`), Jaffna (`BRANCH-JAF`),
Kandy (`BRANCH-KDY`).

## Quote Examples (real values)

### Local delivery — iPhone from Colombo to Colombo
1 item, no weight, same city (surcharge 500): `100 + 25 + 500 = 625`.
```bash
curl -X POST "http://localhost:8082/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-COL",
    "destinationAddress": { "line1": "12 Marine Drive", "city": "Colombo", "area": "Colombo 04", "country": "Sri Lanka" },
    "items": [ { "itemId": "IPHONE-15-001", "quantity": 1 } ]
  }'
```
```json
{ "quoteId": "…", "shippingCost": 625.00, "currency": "LKR" }
```

### Long-haul delivery — iPhone from Jaffna to Colombo
1 item, ~400 km (surcharge 20,000): `100 + 25 + 20000 = 20125`.
```bash
curl -X POST "http://localhost:8082/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-JAF",
    "destinationAddress": { "line1": "12 Marine Drive", "city": "Colombo", "area": "Colombo 04", "country": "Sri Lanka" },
    "items": [ { "itemId": "IPHONE-15-001", "quantity": 1 } ]
  }'
```
```json
{ "quoteId": "…", "shippingCost": 20125.00, "currency": "LKR" }
```

> **Landed-cost flip:** the iPhone is cheaper on the sticker at Jaffna (340,000)
> than Colombo (350,000), but once shipping is added the winner reverses:
> Jaffna 340,000 + 20,125 = **360,125** vs Colombo 350,000 + 625 = **350,625**.
> Colombo is cheaper delivered.

### Multi-item — 3 MacBook Neo from Colombo to Colombo
3 items, local (surcharge 500): `100 + 75 + 500 = 675`.
```bash
curl -X POST "http://localhost:8082/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-COL",
    "destinationAddress": { "line1": "42 Galle Road", "city": "Colombo", "area": "Colombo 03", "country": "Sri Lanka" },
    "items": [ { "itemId": "MACBOOK-NEO-001", "quantity": 3 } ]
  }'
```
```json
{ "quoteId": "…", "shippingCost": 675.00, "currency": "LKR" }
```

### International — MacBook Neo from Colombo to Dubai
1 item, unknown city defaults to 150 km (surcharge 5,000), then ×2.5:
`(100 + 25 + 5000) × 2.5 = 12812.50`.
```bash
curl -X POST "http://localhost:8082/quotes" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-COL",
    "destinationAddress": { "line1": "1 Sheikh Zayed Rd", "city": "Dubai", "area": "Downtown", "country": "UAE" },
    "items": [ { "itemId": "MACBOOK-NEO-001", "quantity": 1 } ]
  }'
```
```json
{ "quoteId": "…", "shippingCost": 12812.50, "currency": "LKR" }
```

---

## Confirm Shipping

### Confirm with a Valid Quote
Use a `quoteId` returned by one of the quote calls above.
```bash
curl -X POST "http://localhost:8082/confirmations" \
  -H "Content-Type: application/json" \
  -d '{
    "quoteId": "REPLACE-WITH-A-REAL-QUOTE-ID",
    "orderId": "ORDER-001",
    "customerId": "CUST-001"
  }'
```
```json
{ "shipmentId": "660e8400-e29b-41d4-a716-446655440000", "status": "CONFIRMED" }
```

### Confirm with an Invalid Quote (404)
```bash
curl -X POST "http://localhost:8082/confirmations" \
  -H "Content-Type: application/json" \
  -d '{
    "quoteId": "INVALID-QUOTE-ID",
    "orderId": "ORDER-002",
    "customerId": "CUST-002"
  }'
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Quote not found or already used: INVALID-QUOTE-ID"
}
```

> A quote is single-use and expires 24 hours after it is created. Confirming an
> expired quote returns **410 Gone**.

---

## PowerShell Commands (Windows)

### Calculate Shipping Quote
```powershell
$body = @{
    branchId = "BRANCH-JAF"
    destinationAddress = @{
        line1 = "12 Marine Drive"
        city = "Colombo"
        area = "Colombo 04"
        country = "Sri Lanka"
    }
    items = @( @{ itemId = "IPHONE-15-001"; quantity = 1 } )
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri "http://localhost:8082/quotes" -Method Post -Body $body -ContentType "application/json"
```

### Confirm Shipping
```powershell
$body = @{
    quoteId = "REPLACE-WITH-A-REAL-QUOTE-ID"
    orderId = "ORDER-001"
    customerId = "CUST-001"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/confirmations" -Method Post -Body $body -ContentType "application/json"
```
