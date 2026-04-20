package com.ta.managementproject.exception;

public class UnprocessableContentException extends RuntimeException{
    public UnprocessableContentException(String message) {
        super(message);
    }
}
