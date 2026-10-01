package org.example.pft.exception;

import org.springframework.http.HttpStatus;

public class EmailSendException extends BusinessException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSendException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
