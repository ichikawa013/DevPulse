package com.devpulse.user_service.exception;

public class BadRequestHandler extends RuntimeException{
    public BadRequestHandler(String msg){
        super(msg);
    }
}
