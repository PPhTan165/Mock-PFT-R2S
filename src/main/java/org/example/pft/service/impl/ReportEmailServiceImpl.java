package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.email.EmailExportRequest;
import org.example.pft.dto.email.EmailExportResponse;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.entity.User;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.service.EmailService;
import org.example.pft.service.PdfExportService;
import org.example.pft.service.ReportEmailService;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Service
@AllArgsConstructor
public class ReportEmailServiceImpl implements ReportEmailService {
    private static final String SUBJECT = "Monthly Financial Report";
    private static final String BODY = "Please find your monthly financial report attached.";

    private final PdfExportService pdfExportService;
    private final EmailService emailService;
    private final CurrentUserHelper currentUserHelper;

    @Override
    public EmailExportResponse sendSummaryReport(EmailExportRequest request) {
        if (request == null) {
            throw new BusinessValidationException("Email export request is required");
        }

        User user = currentUserHelper.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new BusinessValidationException("Authenticated user is required to send report email");
        }

        PdfExportRequest pdfRequest = toPdfExportRequest(request);
        byte[] pdf = pdfExportService.generateSummaryPdf(pdfRequest);

        emailService.sendReport(
                request.getEmail(),
                SUBJECT,
                BODY,
                pdf,
                buildFileName(user.getId(), request.getMonth(), request.getYear())
        );

        return new EmailExportResponse(
                true,
                "Report sent successfully to " + request.getEmail()
        );
    }

    private PdfExportRequest toPdfExportRequest(EmailExportRequest request) {
        PdfExportRequest pdfRequest = new PdfExportRequest();
        pdfRequest.setMonth(request.getMonth());
        pdfRequest.setYear(request.getYear());
        pdfRequest.setIncludeChart(Boolean.TRUE.equals(request.getIncludeChart()));
        pdfRequest.setIncludeTopExpenses(Boolean.TRUE.equals(request.getIncludeTopExpenses()));
        pdfRequest.setReportType(ReportType.SUMMARY);
        return pdfRequest;
    }

    private String buildFileName(Long userId, Integer month, Integer year) {
        return String.format(
                "%d_summary_%s_%d.pdf",
                userId,
                Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toLowerCase(Locale.ENGLISH),
                year
        );
    }
}
