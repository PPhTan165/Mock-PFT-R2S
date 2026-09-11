package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.monthly.SummaryMonthlyData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.entity.User;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.FileExportException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.impl.PdfExportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceImplTest {

    private static final Integer MONTH = 9;
    private static final Integer YEAR = 2026;
    private static final Long USER_ID = 987654321L;

    @Mock
    ReportService reportService;

    @Mock
    CurrentUserHelper currentUserHelper;

    @Mock
    ChartService chartService;

    PdfExportServiceImpl pdfExportService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(USER_ID);

        pdfExportService = new PdfExportServiceImpl(
                reportService,
                currentUserHelper,
                new PdfReportHelper(),
                chartService
        );
    }

    @Test
    void exportPDF_withInvalidRequest_shouldThrowValidationExceptionBeforeAuthentication() {
        PdfExportRequest request = validRequest();
        request.setMonth(13);

        assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withoutAuthenticatedUser_shouldNotFetchReportData() {
        PdfExportRequest request = validRequest();
        when(currentUserHelper.getCurrentUser())
                .thenThrow(new RuntimeException("Unauthenticated"));

        assertThrows(
                RuntimeException.class,
                () -> pdfExportService.exportPDF(request)
        );

        verifyNoInteractions(reportService, chartService);
    }

    @Test
    void exportPDF_withNullReportType_shouldDefaultToSummary() throws Exception {
        PdfExportRequest request = validRequest();
        request.setReportType((ReportType) null);
        Path targetPath = Path.of("reports", USER_ID + "_summary_sep_2026.pdf");
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(MONTH, YEAR))
                .thenReturn(summaryResponse());
        when(reportService.showMonthly(MONTH, YEAR))
                .thenReturn(monthlyResponse());

        try {
            pdfExportService.exportPDF(request);
        } finally {
            Files.deleteIfExists(targetPath);
        }

        verify(reportService).showSummary(MONTH, YEAR);
        verify(reportService).showMonthly(MONTH, YEAR);
    }

    @Test
    void exportPDF_whenTargetFileCannotBeReplaced_shouldThrowFileExportException() throws Exception {
        PdfExportRequest request = validRequest();
        Path targetPath = Path.of("reports", USER_ID + "_summary_sep_2026.pdf");

        Files.deleteIfExists(targetPath);
        Files.createDirectories(targetPath);
        try {
            when(currentUserHelper.getCurrentUser())
                    .thenReturn(user);
            when(reportService.showSummary(MONTH, YEAR))
                    .thenReturn(summaryResponse());
            when(reportService.showMonthly(MONTH, YEAR))
                    .thenReturn(monthlyResponse());

            assertThrows(
                    FileExportException.class,
                    () -> pdfExportService.exportPDF(request)
            );

            verify(reportService).showSummary(MONTH, YEAR);
            verify(reportService).showMonthly(MONTH, YEAR);
            verify(chartService, never()).createIncomeExpenseChart(null);
        } finally {
            Files.deleteIfExists(targetPath);
        }
    }

    private PdfExportRequest validRequest() {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(MONTH);
        request.setYear(YEAR);
        request.setReportType(ReportType.SUMMARY);
        request.setIncludeChart(false);
        request.setIncludeTopExpenses(false);
        return request;
    }

    private ReportResponse<SummaryData> summaryResponse() {
        ReportResponse<SummaryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new SummaryData(
                "September",
                YEAR.shortValue(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()
        ));
        return response;
    }

    private ReportResponse<MonthlyData> monthlyResponse() {
        ReportResponse<MonthlyData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new MonthlyData(
                List.of(new ChartData("SEP", BigDecimal.ZERO, BigDecimal.ZERO)),
                new SummaryMonthlyData("September 2026", BigDecimal.ZERO, BigDecimal.ZERO)
        ));
        return response;
    }
}
