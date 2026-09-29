package org.example.pft.service;

import com.lowagie.text.Document;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.monthly.SummaryMonthlyData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.FileExportException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.impl.PdfExportServiceImpl;
import org.example.pft.service.pdf.PdfOptionalSectionRenderer;
import org.example.pft.service.pdf.PdfExportContext;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Month must be between 1 and 12", exception.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withNullRequest_shouldThrowValidationExceptionBeforeAuthentication() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(null)
        );

        assertEquals("PDF export request is required", exception.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void generateSummaryPdf_withNullRequest_shouldThrowValidationExceptionBeforeAuthentication() {
        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.generateSummaryPdf(null)
        );

        assertEquals("PDF export request is required", exception.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withMissingMonth_shouldThrowValidationExceptionBeforeAuthentication() {
        PdfExportRequest request = validRequest();
        request.setMonth(null);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Month is required", exception.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withMissingYear_shouldThrowValidationExceptionBeforeAuthentication() {
        PdfExportRequest request = validRequest();
        request.setYear(null);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Year is required", exception.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withInvalidYear_shouldThrowValidationExceptionBeforeAuthentication() {
        PdfExportRequest beforeSupportedRange = request(MONTH, 1899, ReportType.SUMMARY);
        PdfExportRequest afterSupportedRange = request(MONTH, 10000, ReportType.SUMMARY);

        BusinessValidationException lowerException = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(beforeSupportedRange)
        );
        BusinessValidationException upperException = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(afterSupportedRange)
        );

        assertEquals("Year must be between 1900 and 9999", lowerException.getMessage());
        assertEquals("Year must be between 1900 and 9999", upperException.getMessage());
        verifyNoInteractions(currentUserHelper, reportService, chartService);
    }

    @Test
    void exportPDF_withUnsupportedReportType_shouldThrowValidationExceptionBeforeAuthentication() {
        PdfExportRequest request = validRequest();
        request.setReportType("WEEKLY");

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Report type must be one of: SUMMARY, MONTHLY, CATEGORY", exception.getMessage());
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
    void exportPDF_withNullCurrentUser_shouldThrowValidationExceptionBeforeReportData() {
        PdfExportRequest request = validRequest();
        when(currentUserHelper.getCurrentUser())
                .thenReturn(null);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Authenticated user is required to export PDF", exception.getMessage());
        verifyNoInteractions(reportService, chartService);
    }

    @Test
    void exportPDF_withCurrentUserMissingId_shouldThrowValidationExceptionBeforeReportData() {
        PdfExportRequest request = validRequest();
        User userWithoutId = new User();
        when(currentUserHelper.getCurrentUser())
                .thenReturn(userWithoutId);

        BusinessValidationException exception = assertThrows(
                BusinessValidationException.class,
                () -> pdfExportService.exportPDF(request)
        );

        assertEquals("Authenticated user is required to export PDF", exception.getMessage());
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
    void generateSummaryPdf_withSupportedOptionalSection_shouldRenderOptionalSection() throws Exception {
        PdfOptionalSectionRenderer optionalSectionRenderer = mock(PdfOptionalSectionRenderer.class);
        PdfExportServiceImpl service = serviceWith(
                List.of(new SummaryPdfRenderer(new PdfReportHelper(), chartService)),
                List.of(optionalSectionRenderer)
        );
        PdfExportRequest request = validRequest();

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(MONTH, YEAR))
                .thenReturn(summaryResponseWithTopExpenses());
        when(reportService.showMonthly(MONTH, YEAR))
                .thenReturn(monthlyResponse());
        when(optionalSectionRenderer.supports(any(PdfExportContext.class)))
                .thenReturn(true);

        byte[] pdf = service.generateSummaryPdf(request);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
        verify(optionalSectionRenderer).supports(any(PdfExportContext.class));
        verify(optionalSectionRenderer).render(any(Document.class), any(PdfExportContext.class));
    }

    @Test
    void generateSummaryPdf_withValidRequest_shouldReturnPdfBytes() {
        PdfExportRequest request = validRequest();
        request.setIncludeChart(null);
        request.setIncludeTopExpenses(true);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(MONTH, YEAR))
                .thenReturn(summaryResponse());
        when(reportService.showMonthly(MONTH, YEAR))
                .thenReturn(monthlyResponse());

        byte[] pdf = pdfExportService.generateSummaryPdf(request);

        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
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
    void exportPDF_withNullCategoryReportResponses_shouldReturnNoCategoryDataMessage() throws Exception {
        PdfExportRequest request = request(5, 2025, ReportType.CATEGORY);
        Path targetPath = Path.of("reports", USER_ID + "_category_may_2025.pdf");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(5, 2025))
                .thenReturn(null);
        when(reportService.showMonthly(5, 2025))
                .thenReturn(monthlyResponse(null));
        when(reportService.showReportCategory(5, 2025, CategoryType.EXPENSE))
                .thenReturn(null);
        when(reportService.showReportCategory(5, 2025, CategoryType.INCOME))
                .thenReturn(categoryResponse(CategoryType.INCOME, null));

        try {
            ReportResponse<String> response = pdfExportService.exportPDF(request);

            assertTrue(response.isSuccess());
            assertEquals("No category data found for May 2025", response.getMessage());
            assertNotNull(response.getData());
        } finally {
            Files.deleteIfExists(targetPath);
        }
    }

    @Test
    void exportPDF_withNullCategoryResponseData_shouldReturnNoCategoryDataMessage() throws Exception {
        PdfExportRequest request = request(6, 2025, ReportType.CATEGORY);
        Path targetPath = Path.of("reports", USER_ID + "_category_jun_2025.pdf");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(6, 2025))
                .thenReturn(summaryResponse(6, 2025, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showMonthly(6, 2025))
                .thenReturn(monthlyResponse());
        when(reportService.showReportCategory(6, 2025, CategoryType.EXPENSE))
                .thenReturn(categoryResponseWithData(null));
        when(reportService.showReportCategory(6, 2025, CategoryType.INCOME))
                .thenReturn(categoryResponse(CategoryType.INCOME, List.of()));

        try {
            ReportResponse<String> response = pdfExportService.exportPDF(request);

            assertEquals("No category data found for June 2025", response.getMessage());
        } finally {
            Files.deleteIfExists(targetPath);
        }
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
    void exportPDF_withMonthlyReportAndNullMonthlyResponse_shouldReturnNoFinancialDataMessage() throws Exception {
        PdfExportRequest request = request(7, 2025, ReportType.MONTHLY);
        Path targetPath = Path.of("reports", USER_ID + "_monthly_jul_2025.pdf");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(reportService.showSummary(7, 2025))
                .thenReturn(summaryResponse(7, 2025, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showSummary(6, 2025))
                .thenReturn(summaryResponse(6, 2025, BigDecimal.ZERO, BigDecimal.ZERO));
        when(reportService.showMonthly(7, 2025))
                .thenReturn(null);

        try {
            ReportResponse<String> response = pdfExportService.exportPDF(request);

            assertEquals("No financial data found for July 2025", response.getMessage());
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

    private PdfExportServiceImpl serviceWith(
            List<PdfReportRenderer> reportRenderers,
            List<PdfOptionalSectionRenderer> optionalSectionRenderers
    ) {
        return new PdfExportServiceImpl(
                reportService,
                currentUserHelper,
                new PdfReportHelper(),
                reportRenderers,
                optionalSectionRenderers
        );
    }

    private ReportResponse<SummaryData> summaryResponse() {
        return summaryResponse(MONTH, YEAR, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private ReportResponse<SummaryData> summaryResponseWithTopExpenses() {
        ReportResponse<SummaryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new SummaryData(
                "September",
                YEAR.shortValue(),
                new BigDecimal("1000.00"),
                new BigDecimal("250.00"),
                new BigDecimal("750.00"),
                List.of(new TopExpenses("Food", null, null, new BigDecimal("120.00")))
        ));
        return response;
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
        return monthlyResponse(new MonthlyData(
                List.of(new ChartData("SEP", BigDecimal.ZERO, BigDecimal.ZERO)),
                new SummaryMonthlyData("September 2026", BigDecimal.ZERO, BigDecimal.ZERO)
        ));
    }

    private ReportResponse<MonthlyData> monthlyResponse(MonthlyData data) {
        ReportResponse<MonthlyData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    private ReportResponse<ReportCategoryData> categoryResponse(
            CategoryType type,
            List<ReportCategory> categories) {
        ReportResponse<ReportCategoryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(new ReportCategoryData(
                type,
                categories == null ? BigDecimal.ZERO : categories.stream()
                        .map(ReportCategory::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                categories
        ));
        return response;
    }

    private ReportResponse<ReportCategoryData> categoryResponseWithData(
            ReportCategoryData data
    ) {
        ReportResponse<ReportCategoryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
    }
}
