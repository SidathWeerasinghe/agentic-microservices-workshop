# Inventory Microservice - cURL Commands

Sample stock lives across four Sri Lankan branches: **Colombo** (`BRANCH-COL`),
**Gampaha** (`BRANCH-GAM`), **Jaffna** (`BRANCH-JAF`), **Kandy** (`BRANCH-KDY`).

## Item Catalog API (Find Item Codes)

### Get All Items Catalog
Returns all items across all branches. Use this to find item codes by product name.

```bash
curl -X GET "http://localhost:8080/stock/catalog"
```

**Expected Response:**
```json
[
    {
        "itemId": "MACBOOK-PRO-001",
        "itemName": "Apple MacBook Pro 14-inch M3",
        "branches": [
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 10, "unitPrice": 600000.00, "status": "IN_STOCK" }
        ]
    },
    {
        "itemId": "MACBOOK-AIR-001",
        "itemName": "Apple MacBook Air 13-inch M2",
        "branches": [
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 0, "unitPrice": 450000.00, "status": "OUT_OF_STOCK" },
            { "branchId": "BRANCH-GAM", "branchName": "Gampaha", "availableQuantity": 15, "unitPrice": 450000.00, "status": "IN_STOCK" }
        ]
    },
    {
        "itemId": "MACBOOK-NEO-001",
        "itemName": "Apple MacBook Neo 12-inch",
        "branches": [
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 20, "unitPrice": 350000.00, "status": "IN_STOCK" }
        ]
    },
    {
        "itemId": "GALAXY-S24-001",
        "itemName": "Samsung Galaxy S24 Ultra 256GB",
        "branches": [
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 25, "unitPrice": 360000.00, "status": "IN_STOCK" },
            { "branchId": "BRANCH-JAF", "branchName": "Jaffna", "availableQuantity": 30, "unitPrice": 320000.00, "status": "IN_STOCK" }
        ]
    },
    {
        "itemId": "DELL-XPS-001",
        "itemName": "Dell XPS 15 Laptop Intel i7",
        "branches": [
            { "branchId": "BRANCH-GAM", "branchName": "Gampaha", "availableQuantity": 15, "unitPrice": 280000.00, "status": "IN_STOCK" },
            { "branchId": "BRANCH-KDY", "branchName": "Kandy", "availableQuantity": 20, "unitPrice": 310000.00, "status": "IN_STOCK" }
        ]
    },
    {
        "itemId": "IPHONE-15-001",
        "itemName": "Apple iPhone 15 Pro 256GB",
        "branches": [
            { "branchId": "BRANCH-JAF", "branchName": "Jaffna", "availableQuantity": 40, "unitPrice": 340000.00, "status": "IN_STOCK" },
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 35, "unitPrice": 350000.00, "status": "IN_STOCK" }
        ]
    }
]
```

---

### Search Items by Name (Natural Language)
Search for items using natural language. Matches item ID or item name (case-insensitive).

```bash
curl -X GET "http://localhost:8080/stock/catalog/search?q=macbook"
curl -X GET "http://localhost:8080/stock/catalog/search?q=galaxy"
curl -X GET "http://localhost:8080/stock/catalog/search?q=dell"
curl -X GET "http://localhost:8080/stock/catalog/search?q=iphone"
```

**Expected Response (search for "iphone"):**
```json
[
    {
        "itemId": "IPHONE-15-001",
        "itemName": "Apple iPhone 15 Pro 256GB",
        "branches": [
            { "branchId": "BRANCH-JAF", "branchName": "Jaffna", "availableQuantity": 40, "unitPrice": 340000.00, "status": "IN_STOCK" },
            { "branchId": "BRANCH-COL", "branchName": "Colombo", "availableQuantity": 35, "unitPrice": 350000.00, "status": "IN_STOCK" }
        ]
    }
]
```

---

## Available Products

| Item ID | Item Name | Branches (qty @ LKR) |
|---------|-----------|----------------------|
| MACBOOK-PRO-001 | Apple MacBook Pro 14-inch M3 | Colombo (10 @ 600,000) |
| MACBOOK-AIR-001 | Apple MacBook Air 13-inch M2 | Colombo (0 @ 450,000), Gampaha (15 @ 450,000) |
| MACBOOK-NEO-001 | Apple MacBook Neo 12-inch | Colombo (20 @ 350,000) |
| GALAXY-S24-001 | Samsung Galaxy S24 Ultra 256GB | Colombo (25 @ 360,000), Jaffna (30 @ 320,000) |
| DELL-XPS-001 | Dell XPS 15 Laptop Intel i7 | Gampaha (15 @ 280,000), Kandy (20 @ 310,000) |
| IPHONE-15-001 | Apple iPhone 15 Pro 256GB | Jaffna (40 @ 340,000), Colombo (35 @ 350,000) |

---

## Stock Check Scenarios

### MacBook Pro at Colombo (IN_STOCK, 10 units @ 600,000)
```bash
curl -X GET "http://localhost:8080/stock?itemId=MACBOOK-PRO-001&branchId=BRANCH-COL"
```
```json
{ "itemId": "MACBOOK-PRO-001", "branchId": "BRANCH-COL", "availableQuantity": 10, "unitPrice": 600000.00, "status": "IN_STOCK" }
```

### MacBook Air at Colombo (OUT_OF_STOCK)
```bash
curl -X GET "http://localhost:8080/stock?itemId=MACBOOK-AIR-001&branchId=BRANCH-COL"
```
```json
{ "itemId": "MACBOOK-AIR-001", "branchId": "BRANCH-COL", "availableQuantity": 0, "unitPrice": 450000.00, "status": "OUT_OF_STOCK" }
```
> The same MacBook Air is available at Gampaha (`BRANCH-GAM`, 15 units).

### iPhone 15 Pro — the "landed cost flip"
Jaffna is **cheaper on the sticker** (340,000) but far from Colombo; Colombo is
pricier (350,000) but local. Once shipping is added, Colombo wins — see the
Shipping microservice for the quote maths.

```bash
curl -X GET "http://localhost:8080/stock?itemId=IPHONE-15-001&branchId=BRANCH-JAF"
curl -X GET "http://localhost:8080/stock?itemId=IPHONE-15-001&branchId=BRANCH-COL"
```

### Stock not found (404)
```bash
curl -X GET "http://localhost:8080/stock?itemId=NO-SUCH-ITEM&branchId=BRANCH-COL"
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Stock not found for item 'NO-SUCH-ITEM' at branch 'BRANCH-COL'"
}
```

---

## Production Order Endpoints

### Create Production Order (Success)
Requested quantity within available stock. Succeeds and reduces branch stock.
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
```json
{ "productionOrderId": "550e8400-e29b-41d4-a716-446655440000", "status": "CREATED" }
```

### Create Production Order — Insufficient Stock (409)
Colombo holds only 10 MacBook Pro; requesting 15 is rejected.
```bash
curl -X POST "http://localhost:8080/production-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "itemId": "MACBOOK-PRO-001",
    "branchId": "BRANCH-COL",
    "requestedQuantity": 15,
    "reason": "Bulk order"
  }'
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 409,
    "error": "Insufficient Stock",
    "message": "Requested quantity 15 exceeds available stock 10 for item 'MACBOOK-PRO-001' at branch 'BRANCH-COL'"
}
```

### Create Production Order — Validation Error (400)
Missing required `itemId`.
```bash
curl -X POST "http://localhost:8080/production-orders" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "BRANCH-COL",
    "requestedQuantity": 5
  }'
```
```json
{
    "timestamp": "2026-08-26T10:00:00",
    "status": 400,
    "error": "Validation Failed",
    "message": "Invalid request parameters",
    "details": { "itemId": "Item ID is required" }
}
```

---

## PowerShell Commands (Windows)

### Check Stock
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/stock?itemId=MACBOOK-PRO-001&branchId=BRANCH-COL" -Method Get
```

### Create Production Order
```powershell
$body = @{
    itemId = "MACBOOK-NEO-001"
    branchId = "BRANCH-COL"
    requestedQuantity = 5
    reason = "Order ORD-1001 - allocate stock"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/production-orders" -Method Post -Body $body -ContentType "application/json"
```
