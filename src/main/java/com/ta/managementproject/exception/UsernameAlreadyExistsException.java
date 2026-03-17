package com.ta.managementproject.exception;

public class UsernameAlreadyExistsException extends RuntimeException{
    public UsernameAlreadyExistsException(String username){
        super(String.format("Username %s sudah digunakan!", username));
    }
}
