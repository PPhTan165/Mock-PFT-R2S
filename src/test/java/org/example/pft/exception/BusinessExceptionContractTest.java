package org.example.pft.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class BusinessExceptionContractTest {

    @Test
    void emailSendException_withCause_shouldPreserveMessageCauseAndStatus() {
        RuntimeException cause = new RuntimeException("smtp timeout");

        EmailSendException exception = new EmailSendException("Failed to send report email", cause);

        assertEquals("Failed to send report email", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
    }

    @Test
    void fileExportException_shouldPreserveMessageAndConflictStatus() {
        FileExportException exception = new FileExportException("PDF generation failed");

        assertEquals("PDF generation failed", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }
}
