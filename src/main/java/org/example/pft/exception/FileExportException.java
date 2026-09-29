package org.example.pft.exception;

import org.springframework.http.HttpStatus;

public class FileExportException extends BusinessException {
    public FileExportException(String message) {
        super(message);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
