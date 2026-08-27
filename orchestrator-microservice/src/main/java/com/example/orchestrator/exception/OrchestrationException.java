package com.example.orchestrator.exception;

import com.example.orchestrator.dto.OrderResponse;
import lombok.Getter;

@Getter
public class OrchestrationException extends RuntimeException {

    private final String failedStep;
    private final OrderResponse partialResponse;

    public OrchestrationException(String message, String failedStep, OrderResponse partialResponse) {
        super(message);
        this.failedStep = failedStep;
        this.partialResponse = partialResponse;
    }

    public OrchestrationException(String message, String failedStep, OrderResponse partialResponse, Throwable cause) {
        super(message, cause);
        this.failedStep = failedStep;
        this.partialResponse = partialResponse;
    }

}
