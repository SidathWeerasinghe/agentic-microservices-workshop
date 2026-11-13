# Architecting Agentic Microservices
### Test & Validation Protocol

> **IEEE Conference Workshop · Lab Companion**
> Exercise the same business scenarios **two ways** — through the REST orchestrator
> (`main` branch, Postman) and through the MCP agent (`agentic` branch, chat) — confirm they
> reach identical outcomes, then see where the agent goes further.

---

## Setup

Restart the services before you begin so the seed data is pristine (the in-memory
database resets on restart, and write scenarios reduce stock).

| Lane | Branch | How to drive it |
|---|---|---|
| **REST** | `main` (or `agentic`) | Start inventory `:8080`, payment `:8081`, shipping `:8082`, orchestrator `:8083`. Import each `sample-data/postman-collection.json`. |
| **MCP** | `agentic` | Start inventory `:8080`, payment `:8081`, shipping `:8082`. Start n8n (`n8n/docker-compose.yml`), import `n8n/agentic-commerce-workflow.json`, set your LLM credential, then chat. |

**Reading a test case.** Each case has a **REST lane** (Postman requests to run) and an
**MCP lane** (a prompt to paste). The **Expected** line is the outcome you should see in
*both*; tick the checkbox as your pass mark.

### Seed data quick reference

| Item ID | Product | Branch: qty @ price (LKR) |
|---|---|---|
| MACBOOK-PRO-001 | MacBook Pro 14" M3 | Colombo: 10 @ 600,000 |
| MACBOOK-AIR-001 | MacBook Air 13" M2 | Colombo: 0 (OUT), Gampaha: 15 @ 450,000 |
| MACBOOK-NEO-001 | MacBook Neo 12" | Colombo: 20 @ 350,000 |
| GALAXY-S24-001 | Galaxy S24 Ultra 256GB | Colombo: 25 @ 360,000, Jaffna: 30 @ 320,000 |
| DELL-XPS-001 | Dell XPS 15 i7 | Gampaha: 15 @ 280,000, Kandy: 20 @ 310,000 |
| IPHONE-15-001 | iPhone 15 Pro 256GB | Jaffna: 40 @ 340,000, Colombo: 35 @ 350,000 |

Run **TC-01 → TC-03** to confirm parity between the two approaches; **TC-04** and
**TC-05** reveal capability the fixed orchestrator does not have.

---

## TC-01 — Place a complete order
**Kind:** read + write · 3 services
**Objective:** verify a full stock → quote → payment → shipping flow completes end to
end, the same way through both surfaces.

**REST lane (Postman)**
1. **Orchestrator** → *Order Scenarios* → **"Success — MacBook Neo x3 (Colombo → Colombo)"** · `POST /orders`
   *(To show the manual chaining instead, run the Inventory → Shipping → Payment → Shipping requests individually.)*

**MCP lane (agent chat)**
```
Hi, I'd like to buy 3 MacBook Neo laptops delivered to 42 Galle Road, Colombo 03,
Western, Sri Lanka. I'll pay by card. Can you give me my order and tracking details?
```

**Expected (both):** stock OK (20 ≥ 3) · shipping **675** · payment **SUCCESS** on
**1,050,675 LKR** · shipment **CONFIRMED** · status **COMPLETED**.

- [ ] Both surfaces returned a completed order with a payment reference and a shipment ID.

---

## TC-02 — High-value order: payment held, then recovered
**Kind:** write · error recovery
**Objective:** a big-ticket order crosses the bank step-up threshold; confirm both
surfaces recover and complete.

**REST lane (Postman)**
1. **Orchestrator** → *Order Scenarios* → **"High-value + auto-retry — MacBook Pro x3"** · `POST /orders`
   *(Recovery here is a hand-coded retry loop, `MAX_PAYMENT_RETRIES = 2`.)*

**MCP lane (agent chat)**
```
I'd like to order 3 MacBook Pro laptops delivered to 88 Reid Avenue, Colombo 07,
Western, Sri Lanka — paying by card.
```

**Expected (both):** total **1,800,675 LKR** (≥ 1.5M) → first charge **HELD**
("Bank authorization hold…") → **retried** → SUCCESS → COMPLETED.

- [ ] Both recovered from the hold; the agent explained the retry in plain language.

---

## TC-03 — Order rejected by a business rule
**Kind:** write · constraint
**Objective:** request more than a branch holds; confirm the constraint is enforced and
observe how each surface reports it.

**REST lane (Postman)**
1. **Orchestrator** → *Order Scenarios* → **"Insufficient Stock — MacBook Pro x15"** · `POST /orders`

**MCP lane (agent chat)**
```
I'd like to buy 15 MacBook Pro laptops for my company, collecting from your Colombo
store. Can you place that for me?
```

**Expected:** Colombo holds only **10** → rejected at the stock step. REST returns
**`status: INSUFFICIENT_STOCK`**; the agent **explains it and proposes a fallback**
(order the available 10, flag the shortfall of 5).

- [ ] The rule was enforced; the agent degraded gracefully rather than just erroring.

---

## TC-04 — Cheapest *landed* cost (the flip) ⭐
**Kind:** read + write · MCP advantage
**Objective:** find the branch that is cheapest *after* delivery. This is where the two
surfaces diverge sharply.

**REST lane (Postman — manual)**
1. **Inventory** → *Price Comparison* → **"iPhone 15 Pro at Jaffna (340,000)"**
2. **Inventory** → *Price Comparison* → **"iPhone 15 Pro at Colombo (350,000)"**
3. **Shipping** → *Quotes* → **"Long-haul — iPhone Jaffna → Colombo (20125)"**
4. **Shipping** → *Quotes* → **"Local — iPhone Colombo → Colombo (625)"**
   *…then **you** add the numbers and pick the winner.*

**MCP lane (agent chat)**
```
I'd like an iPhone 15 Pro delivered to 12 Marine Drive, Colombo 04, Western, Sri Lanka.
Which of your branches works out cheapest once delivery is included? Please place it
with the cheaper option, paying by card.
```

**The maths:**

| Buy from | Sticker | Shipping | Landed total |
|---|---:|---:|---:|
| Jaffna (cheaper sticker) | 340,000 | 20,125 | 360,125 |
| **Colombo (local)** | 350,000 | 625 | **350,625 ✓** |

**Expected:** the **cheaper-priced phone costs 9,500 more delivered.** REST needs 4+
manual calls and human arithmetic to discover this; the agent does it from one sentence
and completes the purchase.

- [ ] The agent chose Colombo (the counter-intuitive winner) and explained the landed-cost maths.

---

## TC-05 — Natural-language product discovery
**Kind:** read · MCP advantage
**Objective:** resolve a vague product request and compare branches — with and without
the language layer.

**REST lane (Postman)**
1. **Inventory** → *Item Catalog* → **"Search Items — Galaxy"** *(then read the JSON and compare prices yourself)*

**MCP lane (agent chat)**
```
A customer wants a Samsung Galaxy phone. Which of your branches have it, and where is
it cheapest?
```

**Expected (both):** Colombo **360,000** vs Jaffna **320,000** → cheapest at **Jaffna**.
REST returns raw JSON to interpret; the agent answers in plain language.

- [ ] Both identified Jaffna as cheapest; the agent needed no item code.

---

## Analysis — what to record

For each test case, note not just the result but the **effort** and **failure behaviour**
of each surface — that contrast is the workshop's takeaway.

| Dimension | REST orchestrator (`main`) | MCP agent (`agentic`) |
|---|---|---|
| Who plans the steps | hard-coded Java | the model, at runtime |
| Passing IDs between calls | manual field mapping | automatic |
| Error recovery | pre-built retry loop | reasoned, on the fly |
| Branch comparison (TC-04) | not supported | native |
| New request shape | new workflow | no change |

> **Reset between runs.** Write scenarios (TC-01, TC-02, TC-04) allocate branch stock through
> production orders and consume single-use quotes. Restart the services before re-running so the numbers match this sheet.

> **Discussion prompt for the room.** TC-01→03 prove parity — so why choose MCP? TC-04
> answers it: the same tools, with a reasoning layer on top, solve problems no one
> pre-programmed. Where else would that change your architecture?

---

*Architecting Agentic Microservices — IEEE Conference Workshop · Test & Validation Protocol.*
*Postman · n8n AI Agent · Model Context Protocol.*
