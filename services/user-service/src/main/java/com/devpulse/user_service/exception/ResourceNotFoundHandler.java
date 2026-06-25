package com.devpulse.user_service.exception;

public class ResourceNotFoundHandler extends RuntimeException{
    public ResourceNotFoundHandler(String message) {
        super(message);
    }
}
