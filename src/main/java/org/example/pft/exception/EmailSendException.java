package org.example.pft.exception;

import org.springframework.http.HttpStatus;

public class EmailSendException extends BusinessException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
