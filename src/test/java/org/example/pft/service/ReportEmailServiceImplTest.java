package org.example.pft.service;

import org.example.pft.dto.email.EmailExportRequest;
import org.example.pft.dto.email.EmailExportResponse;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.entity.User;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.EmailSendException;
import org.example.pft.exception.FileExportException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.service.impl.ReportEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        byte[] pdf = "%PDF".getBytes(StandardCharsets.UTF_8);

        when(currentUserHelper.getCurrentUser()).thenReturn(user);
        when(pdfExportService.generateSummaryPdf(any(PdfExportRequest.class)))
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
                eq("user@example.com"),
                eq("Monthly Financial Report"),
                eq("Please find your monthly financial report attached."),
                attachmentCaptor.capture(),
                eq("2_summary_apr_2024.pdf")
        );
        assertArrayEquals(pdf, attachmentCaptor.getValue());
    }

    @Test
    void sendSummaryReport_withNullRequest_shouldThrowValidationExceptionBeforeAuthentication() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reportEmailService.sendSummaryReport(null)
        );

        assertEquals("Email export request is required", exception.getMessage());
        verifyNoInteractions(currentUserHelper, pdfExportService, emailService);
    }

    @Test
    void sendSummaryReport_whenCurrentUserHelperThrows_shouldPropagateAndSkipPdfAndEmail() {
        EmailExportRequest request = validRequest();
        RuntimeException authenticationException = new RuntimeException("Unauthenticated");

        when(currentUserHelper.getCurrentUser())
                .thenThrow(authenticationException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reportEmailService.sendSummaryReport(request)
        );

        assertSame(authenticationException, exception);
        verifyNoInteractions(pdfExportService, emailService);
    }

    @Test
    void sendSummaryReport_withNullCurrentUser_shouldThrowValidationExceptionBeforePdfGeneration() {
        EmailExportRequest request = validRequest();

        when(currentUserHelper.getCurrentUser())
                .thenReturn(null);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reportEmailService.sendSummaryReport(request)
        );

        assertEquals("Authenticated user is required to send report email", exception.getMessage());
        verifyNoInteractions(pdfExportService, emailService);
    }

    @Test
    void sendSummaryReport_withCurrentUserMissingId_shouldThrowValidationExceptionBeforePdfGeneration() {
        EmailExportRequest request = validRequest();

        when(currentUserHelper.getCurrentUser())
                .thenReturn(new User());

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> reportEmailService.sendSummaryReport(request)
        );

        assertEquals("Authenticated user is required to send report email", exception.getMessage());
        verifyNoInteractions(pdfExportService, emailService);
    }

    @Test
    void sendSummaryReport_whenPdfGenerationFails_shouldPropagateExceptionAndNotSendEmail() {
        EmailExportRequest request = validRequest();
        User user = userWithId(USER_ID);
        FileExportException pdfException = new FileExportException("PDF generation failed");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(pdfExportService.generateSummaryPdf(any(PdfExportRequest.class)))
                .thenThrow(pdfException);

        FileExportException exception = assertThrows(
                FileExportException.class,
                () -> reportEmailService.sendSummaryReport(request)
        );

        assertSame(pdfException, exception);
        verify(pdfExportService).generateSummaryPdf(any(PdfExportRequest.class));
        verify(emailService, never()).sendReport(any(), any(), any(), any(), any());
    }

    @Test
    void sendSummaryReport_whenEmailDeliveryFails_shouldPropagateExceptionAfterPdfGeneration() {
        EmailExportRequest request = validRequest();
        User user = userWithId(USER_ID);
        byte[] pdf = "%PDF".getBytes(StandardCharsets.UTF_8);
        EmailSendException emailException = new EmailSendException("Failed to send report email");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(pdfExportService.generateSummaryPdf(any(PdfExportRequest.class)))
                .thenReturn(pdf);
        org.mockito.Mockito.doThrow(emailException)
                .when(emailService)
                .sendReport(
                        eq("user@example.com"),
                        eq("Monthly Financial Report"),
                        eq("Please find your monthly financial report attached."),
                        eq(pdf),
                        eq("2_summary_apr_2024.pdf")
                );

        EmailSendException exception = assertThrows(
                EmailSendException.class,
                () -> reportEmailService.sendSummaryReport(request)
        );

        assertSame(emailException, exception);
        verify(pdfExportService).generateSummaryPdf(any(PdfExportRequest.class));
        verify(emailService).sendReport(
                eq("user@example.com"),
                eq("Monthly Financial Report"),
                eq("Please find your monthly financial report attached."),
                eq(pdf),
                eq("2_summary_apr_2024.pdf")
        );
    }

    private EmailExportRequest validRequest() {
        EmailExportRequest request = new EmailExportRequest();
        request.setMonth(4);
        request.setYear(2024);
        request.setEmail("user@example.com");
        request.setIncludeChart(null);
        request.setIncludeTopExpenses(true);
        return request;
    }

    private User userWithId(Long userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
