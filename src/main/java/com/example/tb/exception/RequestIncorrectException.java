package com.example.tb.exception;

public class RequestIncorrectException extends RuntimeException{
    private String type;
    public RequestIncorrectException(String type , String message) {
        super(message);
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
