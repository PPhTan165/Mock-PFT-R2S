package org.example.pft.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void handleBindException_withFieldErrors_shouldUseInvalidValueForEachField() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError(
                "request",
                "page",
                "Page must be a number"
        ));
        bindingResult.addError(new FieldError(
                "request",
                "size",
                null
        ));
        BindException exception = new BindException(bindingResult);

        ResponseEntity<ApiError> response = handler.handleBindException(exception);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Validation failed", body.getMessage());
        assertNotNull(body.getErrors());
        assertEquals(2, body.getErrors().size());
        assertEquals("page", body.getErrors().get(0).getField());
        assertEquals("Invalid value", body.getErrors().get(0).getMessage());
        assertEquals("size", body.getErrors().get(1).getField());
        assertEquals("Invalid value", body.getErrors().get(1).getMessage());
    }

    @Test
    void handleInvalidJson_withBooleanInvalidFormatWithoutPath_shouldUseGenericRequestError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        InvalidFormatException cause = InvalidFormatException.from(
                null,
                "Cannot deserialize Boolean",
                "abc",
                Boolean.class
        );
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "Invalid JSON",
                cause,
                null
        );

        ResponseEntity<ApiError> response = handler.handleInvalidJson(exception);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Validation failed", body.getMessage());
        assertNotNull(body.getErrors());
        assertEquals(1, body.getErrors().size());
        assertEquals("request", body.getErrors().get(0).getField());
        assertEquals("Invalid value", body.getErrors().get(0).getMessage());
    }
}
