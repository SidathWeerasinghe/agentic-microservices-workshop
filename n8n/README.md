# n8n agent (agent lane)

The agent that replaces the orchestrator. It talks to the three services over MCP.

1. Start the services from the `agentic` branch (ports 8080, 8081, 8082).
2. `docker compose up -d` in this folder, then open http://localhost:5678 and create the owner account.
3. **Workflows → Import from file** → `agentic-commerce-workflow.json`.
4. Open **Chat Model** and pick your LLM credential (OpenAI by default; any tool-calling chat model works — swap the node if you use another provider).
5. Check the three **MCP Client Tool** nodes: transport *HTTP Streamable*, URLs `http://host.docker.internal:808x/mcp`.
6. Click **Open chat** and paste a prompt from `docs/testing-steps.md`.

Not using Docker? Change the three URLs to `http://localhost:808x/mcp`.

`system-prompt.md` holds the agent's rules (already set inside the workflow).
