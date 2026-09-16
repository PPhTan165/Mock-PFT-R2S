package org.example.pft.service;

import org.example.pft.dto.email.EmailExportRequest;
import org.example.pft.dto.email.EmailExportResponse;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.entity.User;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.service.impl.ReportEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportEmailServiceImplTest {
    private static final Long USER_ID = 2L;

    @Mock
    PdfExportService pdfExportService;

    @Mock
    EmailService emailService;

    @Mock
    CurrentUserHelper currentUserHelper;

    ReportEmailServiceImpl reportEmailService;

    @BeforeEach
    void setup() {
        reportEmailService = new ReportEmailServiceImpl(
                pdfExportService,
                emailService,
                currentUserHelper
        );
    }

    @Test
    void sendSummaryReport_shouldGenerateSummaryPdfAndSendAttachment() {
        EmailExportRequest request = new EmailExportRequest();
        request.setMonth(4);
        request.setYear(2024);
        request.setEmail("user@example.com");
        request.setIncludeChart(null);
        request.setIncludeTopExpenses(true);

        User user = new User();
        user.setId(USER_ID);
        byte[] pdf = "%PDF".getBytes();

        when(currentUserHelper.getCurrentUser()).thenReturn(user);
        when(pdfExportService.generateSummaryPdf(org.mockito.ArgumentMatchers.any(PdfExportRequest.class)))
                .thenReturn(pdf);

        EmailExportResponse response = reportEmailService.sendSummaryReport(request);

        assertEquals(true, response.isSuccess());
        assertEquals("Report sent successfully to user@example.com", response.getMessage());

        ArgumentCaptor<PdfExportRequest> pdfRequestCaptor = ArgumentCaptor.forClass(PdfExportRequest.class);
        verify(pdfExportService).generateSummaryPdf(pdfRequestCaptor.capture());

        PdfExportRequest pdfRequest = pdfRequestCaptor.getValue();
        assertEquals(4, pdfRequest.getMonth());
        assertEquals(2024, pdfRequest.getYear());
        assertEquals(ReportType.SUMMARY, pdfRequest.getReportType());
        assertFalse(pdfRequest.getIncludeChart());
        assertEquals(true, pdfRequest.getIncludeTopExpenses());

        ArgumentCaptor<byte[]> attachmentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(emailService).sendReport(
                org.mockito.ArgumentMatchers.eq("user@example.com"),
                org.mockito.ArgumentMatchers.eq("Monthly Financial Report"),
                org.mockito.ArgumentMatchers.eq("Please find your monthly financial report attached."),
                attachmentCaptor.capture(),
                org.mockito.ArgumentMatchers.eq("2_summary_apr_2024.pdf")
        );
        assertArrayEquals(pdf, attachmentCaptor.getValue());
    }
}
