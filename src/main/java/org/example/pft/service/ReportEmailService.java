package org.example.pft.service;

import org.example.pft.dto.email.EmailExportRequest;
import org.example.pft.dto.email.EmailExportResponse;

public interface ReportEmailService {
    EmailExportResponse sendSummaryReport(EmailExportRequest request);
}
