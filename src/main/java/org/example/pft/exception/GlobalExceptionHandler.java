package org.example.pft.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ApiError> build(HttpStatus status,boolean success, String message, List<ErrorData> errors) {


        ApiError body = new ApiError(
            false,
                "Validation failed",
                errors
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND,false, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req
    ) {
        List<ErrorData> errors = new ArrayList<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.add(new ErrorData(error.getField(),error.getDefaultMessage())));

        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                false,
                "Validation failed",
                errors
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            HttpServletRequest req
    ){
        HttpStatus status = ex.getStatus();

        ApiError error = new ApiError(
                false,
                ex.getMessage(),
                null
        );

        return ResponseEntity.status(status).body(error);
    }


}
