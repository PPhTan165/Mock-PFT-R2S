package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.monthly.SummaryMonthlyData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.FileExportException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.impl.PdfExportServiceImpl;
import org.example.pft.service.pdf.PdfOptionalSectionRenderer;
import org.example.pft.service.pdf.PdfReportRenderer;
import org.example.pft.service.pdf.render.CategoryPdfRenderer;
import org.example.pft.service.pdf.render.MonthlyPdfRenderer;
import org.example.pft.service.pdf.render.SummaryPdfRenderer;
import org.example.pft.service.pdf.section.TopExpensesPdfSectionRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        PdfReportHelper pdfReportHelper = new PdfReportHelper();
        List<PdfReportRenderer> reportRenderers = List.of(
                new SummaryPdfRenderer(pdfReportHelper, chartService),
                new MonthlyPdfRenderer(pdfReportHelper, chartService, reportService),
                new CategoryPdfRenderer(pdfReportHelper, chartService)
        );
        List<PdfOptionalSectionRenderer> optionalSectionRenderers = List.of(
                new TopExpensesPdfSectionRenderer(pdfReportHelper, chartService)
        );

        pdfExportService = new PdfExportServiceImpl(
                reportService,
                currentUserHelper,
                pdfReportHelper,
                reportRenderers,
                optionalSectionRenderers
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

    @Test
    void exportPDF_withCategoryReport_shouldFetchIncomeAndExpenseCategoryData() throws Exception {
        PdfExportRequest request = validRequest();
        request.setReportType(ReportType.CATEGORY);
        request.setIncludeChart(true);
        Path targetPath = Path.of("reports", USER_ID + "_category_sep_2026.pdf");
        List<ReportCategory> expenseCategories = List.of(
                new ReportCategory("Food", new BigDecimal("300.00"), new BigDecimal("75.00")),
                new ReportCategory("Transport", new BigDecimal("100.00"), new BigDecimal("25.00"))
        );
        List<ReportCategory> incomeCategories = List.of(
                new ReportCategory("Salary", new BigDecimal("1000.00"), new BigDecimal("100.00"))
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(MONTH, YEAR))
                .thenReturn(summaryResponse());
        when(reportService.showMonthly(MONTH, YEAR))
                .thenReturn(monthlyResponse());
        when(reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(categoryResponse(CategoryType.EXPENSE, expenseCategories));
        when(reportService.showReportCategory(MONTH, YEAR, CategoryType.INCOME))
                .thenReturn(categoryResponse(CategoryType.INCOME, incomeCategories));

        try {
            pdfExportService.exportPDF(request);
        } finally {
            Files.deleteIfExists(targetPath);
        }

        verify(reportService).showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);
        verify(reportService).showReportCategory(MONTH, YEAR, CategoryType.INCOME);
        verify(chartService).createSquareCategoryChart(expenseCategories, CategoryType.EXPENSE);
        verify(chartService).createSquareCategoryChart(incomeCategories, CategoryType.INCOME);
    }

    @Test
    void exportPDF_withEmptyCategoryReport_shouldReturnNoCategoryDataMessage() throws Exception {
        PdfExportRequest request = request(4, 2024, ReportType.CATEGORY);
        Path targetPath = Path.of("reports", USER_ID + "_category_apr_2024.pdf");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(4, 2024))
                .thenReturn(summaryResponse(4, 2024, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showMonthly(4, 2024))
                .thenReturn(monthlyResponse());
        when(reportService.showReportCategory(4, 2024, CategoryType.EXPENSE))
                .thenReturn(categoryResponse(CategoryType.EXPENSE, List.of()));
        when(reportService.showReportCategory(4, 2024, CategoryType.INCOME))
                .thenReturn(categoryResponse(CategoryType.INCOME, List.of()));

        try {
            ReportResponse<String> response = pdfExportService.exportPDF(request);

            assertEquals("No category data found for April 2024", response.getMessage());
        } finally {
            Files.deleteIfExists(targetPath);
        }
    }

    @Test
    void exportPDF_withEmptyMonthlyReport_shouldReturnNoFinancialDataMessage() throws Exception {
        PdfExportRequest request = request(4, 2024, ReportType.MONTHLY);
        Path targetPath = Path.of("reports", USER_ID + "_monthly_apr_2024.pdf");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(4, 2024))
                .thenReturn(summaryResponse(4, 2024, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showSummary(3, 2024))
                .thenReturn(summaryResponse(3, 2024, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showMonthly(4, 2024))
                .thenReturn(monthlyResponse());

        try {
            ReportResponse<String> response = pdfExportService.exportPDF(request);

            assertEquals("No financial data found for April 2024", response.getMessage());
        } finally {
            Files.deleteIfExists(targetPath);
        }
    }

    private PdfExportRequest validRequest() {
        return request(MONTH, YEAR, ReportType.SUMMARY);
    }

    private PdfExportRequest request(Integer month, Integer year, ReportType reportType) {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(month);
        request.setYear(year);
        request.setReportType(reportType);
        request.setIncludeChart(false);
        request.setIncludeTopExpenses(false);
        return request;
    }

    private ReportResponse<SummaryData> summaryResponse() {
        return summaryResponse(MONTH, YEAR, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private ReportResponse<SummaryData> summaryResponse(
            Integer month,
            Integer year,
            BigDecimal income,
            BigDecimal expense) {
        ReportResponse<SummaryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new SummaryData(
                java.time.Month.of(month).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH),
                year.shortValue(),
                income,
                expense,
                income.subtract(expense),
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

    private ReportResponse<ReportCategoryData> categoryResponse(
            CategoryType type,
            List<ReportCategory> categories) {
        ReportResponse<ReportCategoryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new ReportCategoryData(
                type,
                categories.stream()
                        .map(ReportCategory::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                categories
        ));
        return response;
    }
}
