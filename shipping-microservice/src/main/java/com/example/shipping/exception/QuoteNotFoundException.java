package com.example.shipping.exception;

public class QuoteNotFoundException extends RuntimeException {

    public QuoteNotFoundException(String message) {
        super(message);
    }

    public QuoteNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
