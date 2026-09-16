package org.example.pft.service;

public interface EmailService {
    void sendReport(
            String to,
            String subject,
            String body,
            byte[] attachment,
            String fileName
    );
}
