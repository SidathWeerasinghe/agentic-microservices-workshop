package com.example.shipping.exception;

public class QuoteExpiredException extends RuntimeException {

    public QuoteExpiredException(String message) {
        super(message);
    }

    public QuoteExpiredException(String message, Throwable cause) {
        super(message, cause);
    }

}
