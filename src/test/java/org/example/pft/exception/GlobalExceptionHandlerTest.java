package org.example.pft.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import tools.jackson.databind.exc.InvalidFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

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
