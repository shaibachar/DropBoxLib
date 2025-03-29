package com.dbl.exception;

public class DropBoxLibException extends RuntimeException {
    public DropBoxLibException(String message) {
        super(message);
    }

    public DropBoxLibException(String message, Exception e) {
        super(message, e);
    }
}
