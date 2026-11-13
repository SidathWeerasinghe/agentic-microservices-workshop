# agentic-microservices-workshop
Explore how to design scalable, resilient, and intelligent microservices powered by AI agents.

IEEE conference pre-workshop — **Architecting Agentic Microservices**.

## Branches

| Branch | What it contains | Use it for |
|---|---|---|
| `main` | Inventory (8080), Payment (8081), Shipping (8082) + a REST **orchestrator** (8083) | The baseline: a workflow hard-coded in Java |
| `agentic` | `main` + an **MCP tool layer** on each service (`/mcp` on the same port) | The AI-native version, driven by an LLM agent in n8n |

`git diff main...agentic` (or the pull request) shows the whole cost of going agentic:
per service, one dependency, a few lines of config, a `*Tools` class and a `McpConfig` class.
Everything else — services, controllers, tests, docs, the orchestrator — is identical.

## Quick start

```bash
# Java 21 + Maven 3.6+
cd inventory-microservice && mvn spring-boot:run      # repeat for payment, shipping
cd orchestrator-microservice && mvn spring-boot:run   # REST lane only
```

Agent lane: `git checkout agentic`, start the three services, then follow `n8n/README.md`.

## Workshop material

- `docs/handbook.md` — concepts, the four-move conversion pattern, tool reference, design principles
- `docs/testing-steps.md` — five test cases run two ways (REST orchestrator vs MCP agent)
- `n8n/` — Docker compose, importable agent workflow and system prompt
