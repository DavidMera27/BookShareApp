package com.bookshare.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BookShareException extends RuntimeException{
    private final HttpStatus status;

    public BookShareException(HttpStatus status, String message){
        super(message);
        this.status = status;
    }
}
