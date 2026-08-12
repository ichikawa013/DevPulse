package com.devpulse.feed_service.exception;

public class ResourceNotFoundHandler extends RuntimeException{
    public ResourceNotFoundHandler(String message) {
        super(message);
    }
}
