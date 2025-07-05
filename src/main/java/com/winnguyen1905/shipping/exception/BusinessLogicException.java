package com.winnguyen1905.shipping.exception;

/**
 * Exception thrown when business logic validation fails
 */
public class BusinessLogicException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public BusinessLogicException(String message) {
        super(message);
    }
    
    public BusinessLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
