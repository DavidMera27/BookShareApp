package com.bookshare.utils;

import com.bookshare.exception.BookShareException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ObjectValidator {

    private final Validator validator;

    public <T> T validate(T object){
        Set<ConstraintViolation<T>> errors = validator.validate(object);
        if(errors.isEmpty()){
            return object;
        }else{
            String message = errors.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new BookShareException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
