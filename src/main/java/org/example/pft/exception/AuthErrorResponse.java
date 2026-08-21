package org.example.pft.exception;

public record AuthErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
