# Architecting Agentic Microservices
### Designing AI-Native Distributed Systems — Workshop Handbook

> **IEEE Conference Workshop · Hands-on Lab**
> Format: hands-on lab · Level: intro → intermediate · Duration: ~90 min
> Stack: Java 21 · Spring Boot 3.5 · Spring AI 1.1.3 · Model Context Protocol

---

## Table of contents

1. [Learning objectives](#1-learning-objectives)
2. [From wiring workflows to stating goals](#2-from-wiring-workflows-to-stating-goals)
3. [Reference architecture](#3-reference-architecture)
4. [The Model Context Protocol in brief](#4-the-model-context-protocol-in-brief)
5. [Repository layout: branches, not folders](#5-repository-layout-branches-not-folders)
6. [Prerequisites & environment](#6-prerequisites--environment)
7. [The conversion pattern — the four moves](#7-the-conversion-pattern--the-four-moves)
8. [Service-by-service guide](#8-service-by-service-guide)
   - [8.1 Inventory Service](#81-inventory-service-port-8080)
   - [8.2 Payment Service](#82-payment-service-port-8081)
   - [8.3 Shipping Service](#83-shipping-service-port-8082)
9. [Build, run & verify](#9-build-run--verify)
10. [Connect the agent (n8n)](#10-connect-the-agent-n8n)
11. [Design principles](#11-design-principles)
12. [Security & production considerations](#12-security--production-considerations)
13. [Recap](#13-recap)

---

## 1. Learning objectives

By the end of this lab you will have taken conventional REST microservices and made
them callable by an autonomous AI agent — changing four files per service and writing
**zero** new business logic.

- **A.** Distinguish *imperative REST orchestration* from *agentic, LLM-driven orchestration*, and know when each is appropriate.
- **B.** Explain what the Model Context Protocol (MCP) is and why a *tool* is the right abstraction for an agent.
- **C.** Convert an existing Spring Boot service into an MCP server with additive, minimal-impact changes.
- **D.** Wire the tools to an n8n AI Agent and reason about tool-description quality, testing, and security.

---

## 2. From wiring workflows to stating goals

Distributed systems have always needed an *orchestrator* — a component that knows the
order of operations. This workshop asks: **who** writes that orchestration, and **when**.

| The classic way — a hand-built orchestrator | The AI-native way — an agent + tool-exposing services |
|---|---|
| A developer writes each service call | Each service advertises its capabilities as tools |
| Field mapping between services is manual | The agent discovers them and plans at runtime |
| Retries and error paths are coded in advance | It threads outputs into inputs and recovers from errors |
| Every new request shape is a new workflow | One agent handles any phrasing of any request |

> **Key idea.** With REST you **wire the workflow**. With MCP you **state the goal**, and
> the agent assembles the workflow itself — from a natural-language request, at runtime.
> Neither replaces the other: deterministic, high-volume paths still belong in code;
> open-ended, human-facing tasks are where agentic orchestration shines.

---

## 3. Reference architecture

The domain is a small electronics retailer with three microservices: **Inventory**,
**Payment**, and **Shipping**. The only structural difference between the two designs is
what sits in front of them.

```
         REST (main branch)                   MCP (agentic branch)
      ┌────────────────────┐                 ┌────────────────────┐
      │    Orchestrator    │                 │     LLM Agent      │
      └─────────┬──────────┘                 └─────────┬──────────┘
      workflow hard-coded in Java             workflow reasoned at runtime
        ┌───────┼───────┐                       ┌───────┼───────┐
        ▼       ▼       ▼                       ▼       ▼       ▼
      ┌────┐ ┌────┐ ┌────┐                   ┌────┐ ┌────┐ ┌────┐
      │Inv │ │Pay │ │Ship│                   │Inv │ │Pay │ │Ship│
      └────┘ └────┘ └────┘                   │/mcp│ │/mcp│ │/mcp│
                                             └────┘ └────┘ └────┘
```

Critically, **the same three services power both designs**. In the MCP version each
service gains a thin tool layer alongside its REST controllers — the business logic is
shared and untouched. That property is what this lab demonstrates.

---

## 4. The Model Context Protocol in brief

MCP is an open protocol that standardises how an application exposes **tools** (callable
functions), **resources** (readable data), and **prompts** to an AI model, so any
compliant client can discover and invoke them.

For this workshop only one concept matters: a **tool** — a named function with a
natural-language *description* and typed parameters. The agent reads the description to
decide *whether* and *how* to call it; the server executes it and returns the result.
The description is, in effect, the contract the model programs against — so writing good
descriptions is a first-class engineering task.

| Handshake step | What happens |
|---|---|
| `initialize` | Client and server agree a protocol version and exchange capabilities |
| `tools/list` | Server advertises each tool's name, description, and input schema |
| `tools/call` | Client invokes a tool by name with arguments; server runs it and returns the result |

Spring AI implements the server side of this protocol. Our job is only to *mark* which
methods are tools and register them — no protocol code required.

---

## 5. Repository layout: branches, not folders

This workshop is delivered as a **single Git repository with two branches**, so the
entire cost of "going agentic" is visible as one diff.

| Branch | Contents | Role |
|---|---|---|
| `main` | inventory, payment, shipping **+ orchestrator** (REST only) | The baseline |
| `agentic` | `main` **+ the MCP tool layer** on each service | The AI-native version |

The orchestrator stays on `agentic` too, so you can run the REST lane and the agent lane
side by side from one checkout. Shared assets (this handbook, the test protocol, and the
`n8n/` agent workflow) live on `main` and are identical on both branches.

### What the `main...agentic` diff contains (per service)

```
 <service>/pom.xml                                   |  + Spring AI BOM + MCP starter
 <service>/src/main/resources/application.properties |  + MCP server config (5 lines)
 <service>/src/main/java/.../mcp/<Service>Tools.java |  new (thin @Tool adapter)
 <service>/src/main/java/.../mcp/McpConfig.java       |  new (one @Bean)
```

Nothing else changes: controllers, services, DTOs, entities, tests, docs and the
orchestrator are identical on both branches. Framework versions (Boot 3.5.11, springdoc 2.8.9) are the same on both
branches, so the diff is **exclusively the MCP additions** — not an incidental upgrade.

> **Why streamable HTTP needs Boot 3.5.** The `/mcp` streamable transport is provided by
> Spring AI 1.1.x, which targets Spring Boot 3.5.x. Keep both branches on the same Boot
> and springdoc versions so the branch diff stays clean.

---

## 6. Prerequisites & environment

- **Java 21+**, **Maven 3.6+**
- A REST client — **Postman** (import each `sample-data/postman-collection.json`) or the bundled `src/test/resources/api-requests.http`
- **Docker** to run **n8n** (`n8n/docker-compose.yml`) with the *MCP Client Tool* node (for the agent portion)
- An **LLM API key** for the n8n chat model (the sample workflow uses OpenAI; any tool-calling chat model works)

| Service | Port | REST base | MCP endpoint |
|---|---|---|---|
| Inventory | 8080 | `http://localhost:8080` | `http://localhost:8080/mcp` |
| Payment | 8081 | `http://localhost:8081` | `http://localhost:8081/mcp` |
| Shipping | 8082 | `http://localhost:8082` | `http://localhost:8082/mcp` |
| Orchestrator (REST lane) | 8083 | `http://localhost:8083` | — |

---

## 7. The conversion pattern — the four moves

Every service is converted with the **same four additive moves**. Section 8 then applies
them to each service with its specific tools.

**Move 1 — add the dependency** (`pom.xml`): import the Spring AI BOM and the WebMVC MCP
server starter.

```xml
<properties>
    <spring-ai.version>1.1.3</spring-ai.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```

**Move 2 — enable the server** (`application.properties`): turn it on and choose the
Streamable HTTP transport (served at `/mcp` on the same port as REST).

```properties
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.name=<service>-mcp
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.protocol=STREAMABLE
```

**Move 3 — write the tool adapter** (`mcp/<Service>Tools.java`): a `@Component` that
injects the *same* service the controller uses; each `@Tool` method delegates one call.
Because it calls the service directly, the controller's `@Valid` does **not** run — so the
adapter validates its arguments itself (inject `jakarta.validation.Validator` and check the
DTO before delegating). Arguments written by an LLM are untrusted input.

**Move 4 — register the tools** (`mcp/McpConfig.java`): expose the adapter as a
`ToolCallbackProvider`, in its **own** `@Configuration` class.

> **Why a separate config class, not the main class.** If the `@Bean` lives in the main
> `@SpringBootApplication` class, every `@WebMvcTest` slice tries to build it and fails
> (the slice does not scan `@Component`s, so the `*Tools` bean is missing). Isolating the
> MCP wiring keeps existing controller tests green.

---

## 8. Service-by-service guide

The tool `description` and each `@ToolParam` description below are the **exact contract**
the LLM reads. They are reproduced verbatim from the code — treat them as API design.

### 8.1 Inventory Service (port 8080)

**Domain.** Stock levels per item per branch, and production orders. A production order
**allocates** stock for a purchase: it deducts the requested quantity from the branch and is
rejected if the branch does not hold enough.
**Underlying beans reused:** `StockService`, `ProductionOrderService`.

**Tool reference**

| Tool | Kind | Description | Parameters |
|---|---|---|---|
| `checkStock` | read | Check stock availability and status for one item at one branch. Returns availableQuantity, unitPrice (LKR) and status (IN_STOCK, LOW_STOCK or OUT_OF_STOCK). Use item and branch IDs returned by searchItems; do not guess them. | `itemId` — e.g. MACBOOK-PRO-001 · `branchId` — e.g. BRANCH-COL |
| `searchItems` | read | Search the item catalog by product name or item ID (case-insensitive). Use this to resolve a natural-language product name into an item ID. Each result lists every branch that stocks the item with its quantity and unit price (LKR). | `q` — search term matching item ID or name |
| `getAllItemsCatalog` | read | List the entire item catalog with stock information across all branches. Prefer searchItems when you already know part of the product name. | *(none)* |
| `createProductionOrder` | **write** | Create a production order that allocates stock for a customer purchase: it DEDUCTS requestedQuantity from the item's available stock at the branch. Rejected if requestedQuantity exceeds available stock. Call once per purchased item, after checking stock and before taking payment. | `request` — itemId, branchId, requestedQuantity (positive), optional reason |

**`application.properties` addition:** `spring.ai.mcp.server.name=inventory-mcp` (plus the shared block from Move 2).

**`src/main/java/com/example/inventory/mcp/InventoryTools.java`**

```java
package com.example.inventory.mcp;

import com.example.inventory.dto.ItemCatalogResponse;
import com.example.inventory.dto.ProductionOrderRequest;
import com.example.inventory.dto.ProductionOrderResponse;
import com.example.inventory.dto.StockResponse;
import com.example.inventory.service.ProductionOrderService;
import com.example.inventory.service.StockService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the inventory domain.
 * <p>
 * These are thin wrappers over the existing {@link StockService} and
 * {@link ProductionOrderService} beans - the same business logic the REST
 * controllers use. No logic is duplicated here; this class only exposes the
 * service layer as MCP tools. The REST controllers remain untouched.
 * <p>
 * Tool arguments come from an LLM, so they are treated as untrusted input: the same
 * Bean Validation rules the REST layer enforces with {@code @Valid} are applied here
 * before the service is called, and a clear message is returned to the agent.
 */
@Component
@RequiredArgsConstructor
public class InventoryTools {

    private final StockService stockService;
    private final ProductionOrderService productionOrderService;
    private final Validator validator;

    @Tool(description = "Check stock availability and status for one item at one branch. "
            + "Returns availableQuantity, unitPrice (LKR) and status (IN_STOCK, LOW_STOCK or OUT_OF_STOCK). "
            + "Use item and branch IDs returned by searchItems; do not guess them.")
    public StockResponse checkStock(
            @ToolParam(description = "Item ID, e.g. MACBOOK-PRO-001") String itemId,
            @ToolParam(description = "Branch ID, e.g. BRANCH-COL") String branchId) {
        requireText(itemId, "itemId");
        requireText(branchId, "branchId");
        return stockService.checkStock(itemId, branchId);
    }

    @Tool(description = "Search the item catalog by product name or item ID (case-insensitive). "
            + "Use this to resolve a natural-language product name into an item ID. "
            + "Each result lists every branch that stocks the item with its quantity and unit price (LKR). "
            + "Example: 'macbook' finds all MacBook products, 'galaxy' finds Samsung Galaxy products.")
    public List<ItemCatalogResponse> searchItems(
            @ToolParam(description = "Search term matching item ID or name") String q) {
        requireText(q, "q");
        return stockService.searchItems(q);
    }

    @Tool(description = "List the entire item catalog with stock information across all branches. "
            + "Prefer searchItems when you already know part of the product name.")
    public List<ItemCatalogResponse> getAllItemsCatalog() {
        return stockService.getAllItemsCatalog();
    }

    @Tool(description = "Create a production order that allocates stock for a customer purchase: "
            + "it DEDUCTS requestedQuantity from the item's available stock at the branch. "
            + "Rejected if requestedQuantity exceeds available stock. Write operation - call it once per "
            + "purchased item, after checking stock and before taking payment. It does not add stock.")
    public ProductionOrderResponse createProductionOrder(
            @ToolParam(description = "Production order details: itemId, branchId, requestedQuantity (positive), "
                    + "and optional reason (e.g. the order ID)")
            ProductionOrderRequest request) {
        return productionOrderService.createProductionOrder(validate(request));
    }

    private <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
```

**`src/main/java/com/example/inventory/mcp/McpConfig.java`**

```java
package com.example.inventory.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Bean
    public ToolCallbackProvider inventoryToolCallbackProvider(InventoryTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
```

---

### 8.2 Payment Service (port 8081)

**Domain.** Process payments and retry failed ones. **Reused bean:** `PaymentService`.

**Behaviour to know (demo simulation).** In deterministic demo mode every charge succeeds,
**except** a card charge of **1,500,000 LKR or more**, which is *held* for bank step-up
authorization on the first attempt (`status: FAILED`, reason "Bank authorization hold…")
and **clears on retry**. Configured by `payment.simulation.deterministic` and
`payment.simulation.step-up-threshold`.

**Tool reference**

| Tool | Kind | Description | Parameters |
|---|---|---|---|
| `makePayment` | **write** | Process a new payment for an order (amount and currency in LKR; paymentMethod CARD, CASH or BANK_TRANSFER). Only one payment per order: if it FAILS, call retryPayment with the paymentReference — never makePayment again. | `request` — orderId, amount, currency (LKR), paymentMethod, optional customerId |
| `retryPayment` | **write** | Retry a previously FAILED payment using its paymentReference. Optionally override the payment method. Fails if the payment already succeeded. | `paymentReference` — reference of the failed payment · `request` — optional paymentMethod and reason |
| `getPayment` | read | Retrieve the details and current status of a payment by its payment reference. | `paymentReference` — the reference to look up |

**`application.properties` addition:** `spring.ai.mcp.server.name=payment-mcp` (plus the shared block from Move 2).

**`src/main/java/com/example/payment/mcp/PaymentTools.java`**

```java
package com.example.payment.mcp;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentRetryRequest;
import com.example.payment.service.PaymentService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the payment domain.
 * <p>
 * Thin wrappers over the existing {@link PaymentService} bean - the same
 * business logic the REST controller uses. The REST controller remains
 * untouched.
 * <p>
 * Tool arguments come from an LLM, so they are validated here with the same Bean
 * Validation rules the REST layer enforces with {@code @Valid}.
 */
@Component
@RequiredArgsConstructor
public class PaymentTools {

    private final PaymentService paymentService;
    private final Validator validator;

    @Tool(description = "Process a new payment for an order. Write operation. "
            + "Requires orderId, amount (positive, in LKR), currency (LKR) and paymentMethod "
            + "(one of CARD, CASH, BANK_TRANSFER). Returns the payment with its paymentReference and status. "
            + "Only one payment is allowed per order: if the status is FAILED, call retryPayment with the "
            + "paymentReference - do NOT call makePayment again for the same order.")
    public PaymentResponse makePayment(
            @ToolParam(description = "Payment details: orderId, amount (LKR), currency (LKR), "
                    + "paymentMethod (CARD|CASH|BANK_TRANSFER), optional customerId")
            PaymentRequest request) {
        return paymentService.makePayment(validate(request));
    }

    @Tool(description = "Retry a previously FAILED payment using its paymentReference. Write operation. "
            + "Optionally override the payment method. Fails if the payment already succeeded.")
    public PaymentResponse retryPayment(
            @ToolParam(description = "The paymentReference of the failed payment to retry") String paymentReference,
            @ToolParam(description = "Optional retry details: paymentMethod (CARD|CASH|BANK_TRANSFER) and reason. "
                    + "May be null to retry as-is.", required = false)
            PaymentRetryRequest request) {
        requireText(paymentReference, "paymentReference");
        return paymentService.retryPayment(paymentReference, request);
    }

    @Tool(description = "Retrieve the details and current status of a payment by its paymentReference.")
    public PaymentResponse getPayment(
            @ToolParam(description = "The paymentReference to look up") String paymentReference) {
        requireText(paymentReference, "paymentReference");
        return paymentService.getPayment(paymentReference);
    }

    private <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
    }
}
```

**`src/main/java/com/example/payment/mcp/McpConfig.java`**

```java
package com.example.payment.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Bean
    public ToolCallbackProvider paymentToolCallbackProvider(PaymentTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
```

---

### 8.3 Shipping Service (port 8082)

**Domain.** Calculate distance-based shipping quotes and confirm shipments.
**Reused bean:** `ShippingService`.

**Behaviour to know.** Cost = `100 + 50/kg + 25/item + distance-zone surcharge`, and the
whole cost is multiplied by **2.5** for international destinations. A quote is single-use
and expires after 24 hours (confirming an expired quote returns 410).

**Tool reference**

| Tool | Kind | Description | Parameters |
|---|---|---|---|
| `calculateShippingQuote` | read | Calculate a shipping cost quote (LKR) from a branch to a destination address; cost depends on the distance between cities. Returns a quoteId needed to confirm shipping. | `request` — branchId, destinationAddress (line1, city, area, country, optional postalCode), items (itemId, quantity; weightKg only if the customer gave one) |
| `confirmShipping` | **write** | Confirm shipping using a previously calculated quote. Call only after the payment succeeded. Fails if the quote does not exist, was already used, or has expired. | `request` — quoteId (from calculateShippingQuote), orderId, customerId |

**`application.properties` addition:** `spring.ai.mcp.server.name=shipping-mcp` (plus the shared block from Move 2).

**`src/main/java/com/example/shipping/mcp/ShippingTools.java`**

```java
package com.example.shipping.mcp;

import com.example.shipping.dto.ShippingConfirmationRequest;
import com.example.shipping.dto.ShippingConfirmationResponse;
import com.example.shipping.dto.ShippingQuoteRequest;
import com.example.shipping.dto.ShippingQuoteResponse;
import com.example.shipping.service.ShippingService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * MCP tool adapters for the shipping domain.
 * <p>
 * Thin wrappers over the existing {@link ShippingService} bean - the same
 * business logic the REST controller uses. The REST controller remains
 * untouched.
 * <p>
 * Tool arguments come from an LLM, so they are validated here with the same Bean
 * Validation rules the REST layer enforces with {@code @Valid}.
 */
@Component
@RequiredArgsConstructor
public class ShippingTools {

    private final ShippingService shippingService;
    private final Validator validator;

    @Tool(description = "Calculate a shipping cost quote (LKR) for items shipped from a branch to a "
            + "destination address. Cost depends on the distance from the branch's city to the destination city. "
            + "Returns a quoteId that is needed later to confirm shipping.")
    public ShippingQuoteResponse calculateShippingQuote(
            @ToolParam(description = "Quote request: branchId (e.g. BRANCH-COL), destinationAddress (line1, city, "
                    + "area, country, optional postalCode), and items (each with itemId and quantity). "
                    + "Leave weightKg out unless the customer gave a weight - never estimate it.")
            ShippingQuoteRequest request) {
        return shippingService.calculateShippingQuote(validate(request));
    }

    @Tool(description = "Confirm shipping using a previously calculated quote. Write operation. "
            + "Call only after the payment succeeded. Fails if the quote does not exist, was already used, "
            + "or has expired. Requires quoteId, orderId and customerId.")
    public ShippingConfirmationResponse confirmShipping(
            @ToolParam(description = "Confirmation request: quoteId (from calculateShippingQuote), orderId, and customerId")
            ShippingConfirmationRequest request) {
        return shippingService.confirmShipping(validate(request));
    }

    private <T> T validate(T request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is required");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid request: " + violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; ")));
        }
        return request;
    }
}
```

**`src/main/java/com/example/shipping/mcp/McpConfig.java`**

```java
package com.example.shipping.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Bean
    public ToolCallbackProvider shippingToolCallbackProvider(ShippingTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
```

---

## 9. Build, run & verify

Per service (from the `agentic` branch):

```bash
mvn spring-boot:run
# The startup log confirms registration, e.g.:
#   McpServerAutoConfiguration : Registered tools: 4   (inventory)
#                                Registered tools: 3   (payment)
#                                Registered tools: 2   (shipping)

# REST still works, unchanged:
curl "http://localhost:8080/stock/catalog/search?q=iphone"      # 200

# MCP is live at /mcp (Streamable HTTP) on the same port.
```

Expected tool counts: **Inventory 4, Payment 3, Shipping 2** — nine tools in total.

---

## 10. Connect the agent (n8n)

Everything for this step is in the `n8n/` folder:

| File | Purpose |
|---|---|
| `n8n/docker-compose.yml` | Runs n8n on `http://localhost:5678`, able to reach services on the host |
| `n8n/agentic-commerce-workflow.json` | Chat trigger → AI Agent → chat model + memory + three MCP Client Tool nodes |
| `n8n/system-prompt.md` | The agent's operating rules (already inside the workflow) |

1. `cd n8n && docker compose up -d`, open `http://localhost:5678` and create the owner account.
2. **Workflows → Import from file** → `agentic-commerce-workflow.json`.
3. Open the chat-model node and select (or create) your LLM credential.
4. Check the three **MCP Client Tool** nodes: transport **HTTP Streamable**, URLs
   `http://host.docker.internal:8080/mcp`, `:8081/mcp`, `:8082/mcp`.
5. Click **Open chat** and paste a prompt from the test protocol.

If you build the workflow by hand instead: add an **MCP Client Tool** node to your
**AI Agent** for each service (transport **HTTP Streamable**). The agent auto-discovers
all nine tools. Give it these operating rules as the system prompt:

```text
You are a customer assistant for an electronics retailer in Sri Lanka.
Customers chat in plain language and never know internal IDs — you create
and manage those yourself. For any purchase:
- Resolve product names to item IDs with searchItems; confirm stock with
  checkStock at the branch you will ship from.
- Generate your own unique order ID (for example ORD-<random>); never ask the
  customer for one. Use the customer's name or email as their customer ID.
- Allocate the stock with createProductionOrder (one per item, reason = the
  order ID) before taking payment.
- Get a shipping quote with calculateShippingQuote. Do not send weightKg
  unless the customer told you the weight.
- Pay with makePayment for (unit price x quantity) + shipping cost, in LKR.
  If the payment status is FAILED, explain briefly and call retryPayment with
  its paymentReference. Never call makePayment twice for the same order.
- Confirm shipping with confirmShipping only after the payment succeeded,
  passing the quoteId forward.
- If several branches stock the item, compare the landed cost (price +
  shipping to the customer's city) when the customer asks for the cheapest.
- Thread IDs from one tool into the next; never invent item IDs, prices,
  quote IDs or payment references — use only tool-returned values.
- If a step fails and you cannot fix it, stop and tell the customer what
  happened, including anything already charged or allocated.
- End by giving the customer their order number, payment reference and
  shipment ID in plain language.
```

> n8n in Docker reaches the host as `host.docker.internal` (the compose file maps it on Linux too).
> Running n8n without Docker? Use `localhost` instead.

---

## 11. Design principles

1. **Tools are an interface for a reasoning system.** Their descriptions are the contract the model programs against. Write them behaviourally ("rejected if quantity exceeds stock"), name parameters clearly, and prefer a small set of meaningful tools over many thin ones.
2. **Expose intent, not implementation.** Choose which service methods become tools deliberately. Read/search tools are low-risk; write tools (payments, orders) deserve extra scrutiny and possibly human-in-the-loop confirmation.
3. **Wrap the service layer, not the HTTP endpoint.** Call the service bean directly — one source of truth and no extra network hop. Domain exceptions surface to the agent as tool errors, but controller-level `@Valid` does not run, so validate tool arguments in the adapter.
4. **Keep the MCP layer additive and isolated.** One adapter class and one config bean per service; the REST surface and its tests should not notice MCP exists.
5. **Mind the missing safety nets.** An agent chaining services has no distributed transaction. If payment succeeds but shipping fails, nothing compensates automatically. Production systems add compensating actions (a refund tool the agent can call) — the agentic equivalent of a saga.

---

## 12. Security & production considerations

- `/mcp` inherits the application's security chain — which by default is **none**. An agent able to call write tools must sit behind authentication and a network boundary.
- Never expose write tools (`makePayment`, `createProductionOrder`, `confirmShipping`) on an unauthenticated public endpoint.
- Prefer least privilege: expose read/search tools broadly; gate writes.
- Log tool invocations for audit; treat the agent as an untrusted caller and keep server-side validation authoritative.
- Make write tools idempotent. An agent may repeat a call; `makePayment` therefore allows one payment per order and points the agent to `retryPayment`.
- Keep arithmetic in tools, not in the model. Here the agent still adds price × quantity + shipping itself before paying — fine for a demo, but a production design would return the payable total from a tool.

---

## 13. Recap

You took three ordinary REST microservices and made them **AI-native** — callable by an
autonomous agent — without rewriting a single line of business logic. On the `agentic`
branch, `git diff main...agentic` shows the entire cost of the change: a dependency, five
lines of config, and two small classes per service.

Continue to the **Test & Validation Protocol** (`docs/testing-steps.md`) to exercise the same
business scenarios two ways — through the REST orchestrator on `main`, and through the
MCP agent on `agentic`.

---

*Architecting Agentic Microservices — IEEE Conference Workshop · Lab Handbook.*
*Spring Boot 3.5 · Spring AI 1.1.3 · Model Context Protocol.*
