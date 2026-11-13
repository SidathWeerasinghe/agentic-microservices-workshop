package com.example.shipping.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link ShippingTools} @Tool methods with the Spring AI MCP server,
 * which exposes them over Streamable HTTP at /mcp on the same port as the REST API.
 * <p>
 * This wiring is kept in a dedicated configuration class (not the main application
 * class) so that web-slice tests such as {@code @WebMvcTest} do not try to create
 * the MCP beans.
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider shippingToolCallbackProvider(ShippingTools shippingTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(shippingTools)
                .build();
    }
}
