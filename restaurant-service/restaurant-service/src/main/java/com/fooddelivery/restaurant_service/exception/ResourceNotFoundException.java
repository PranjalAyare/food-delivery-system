package com.fooddelivery.restaurant_service.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException() {
        super();
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
