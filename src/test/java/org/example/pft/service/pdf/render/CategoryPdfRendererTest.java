package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.pdf.CategoryReportData;
import org.example.pft.service.pdf.PdfExportContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryPdfRendererTest {

    @Mock
    ChartService chartService;

    CategoryPdfRenderer renderer;

    @BeforeEach
    void setup() {
        renderer = new CategoryPdfRenderer(
                new PdfReportHelper(),
                chartService
        );
    }

    @Test
    void supportTypeAndTitle_shouldIdentifyCategoryReport() {
        assertEquals(ReportType.CATEGORY, renderer.supportType());
        assertEquals("Category Report", renderer.title());
    }

    @Test
    void render_withIncomeAndExpenseDataAndChartDisabled_shouldWriteBothCategoryTables() throws Exception {
        List<ReportCategory> expenseCategories = List.of(
                category("Food", "300.00", "60.00"),
                category("Transportation", "125.50", "25.10"),
                category("Entertainment", "74.50", "14.90")
        );
        List<ReportCategory> incomeCategories = List.of(
                category("Salary", "2500.00", "83.33"),
                category("Freelance", "500.00", "16.67")
        );

        byte[] pdf = renderPdf(context(false, expenseCategories, incomeCategories));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thu/chi: EXPENSE"));
        assertTrue(text.contains("Bang thu/chi: INCOME"));
        assertTrue(text.contains("Category"));
        assertTrue(text.contains("Amount"));
        assertTrue(text.contains("Percentage"));
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("$300.00"));
        assertTrue(text.contains("60.00%"));
        assertTrue(text.contains("Transportation"));
        assertTrue(text.contains("$125.50"));
        assertTrue(text.contains("Salary"));
        assertTrue(text.contains("$2,500.00"));
        assertTrue(text.contains("83.33%"));
        assertTrue(text.contains("Freelance"));
        assertTrue(text.contains("$500.00"));
        verifyNoInteractions(chartService);
    }

    @Test
    void render_withEmptyExpenseAndNullIncomeData_shouldWriteFallbackMessages() throws Exception {
        byte[] pdf = renderPdf(context(false, List.of(), null));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thu/chi: EXPENSE"));
        assertTrue(text.contains("No expense category data available"));
        assertTrue(text.contains("Bang thu/chi: INCOME"));
        assertTrue(text.contains("No income category data available"));
        verifyNoInteractions(chartService);
    }

    @Test
    void render_withChartEnabledAndGeneratedCharts_shouldRequestChartsAndRenderPdf() throws Exception {
        List<ReportCategory> expenseCategories = List.of(
                category("Food", "300.00", "75.00")
        );
        List<ReportCategory> incomeCategories = List.of(
                category("Salary", "1000.00", "100.00")
        );
        Image expenseChart = onePixelImage();
        Image incomeChart = onePixelImage();

        when(chartService.createSquareCategoryChart(expenseCategories, CategoryType.EXPENSE))
                .thenReturn(expenseChart);
        when(chartService.createSquareCategoryChart(incomeCategories, CategoryType.INCOME))
                .thenReturn(incomeChart);

        byte[] pdf = renderPdf(context(true, expenseCategories, incomeCategories));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("$300.00"));
        assertTrue(text.contains("Salary"));
        assertTrue(text.contains("$1,000.00"));
        verify(chartService).createSquareCategoryChart(expenseCategories, CategoryType.EXPENSE);
        verify(chartService).createSquareCategoryChart(incomeCategories, CategoryType.INCOME);
    }

    @Test
    void render_withChartEnabledAndNullCharts_shouldKeepCategoryContent() throws Exception {
        List<ReportCategory> expenseCategories = List.of(
                category("Food", "300.00", "75.00")
        );
        List<ReportCategory> incomeCategories = List.of(
                category("Salary", "1000.00", "100.00")
        );

        when(chartService.createSquareCategoryChart(expenseCategories, CategoryType.EXPENSE))
                .thenReturn(null);
        when(chartService.createSquareCategoryChart(incomeCategories, CategoryType.INCOME))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(true, expenseCategories, incomeCategories));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thu/chi: EXPENSE"));
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("Bang thu/chi: INCOME"));
        assertTrue(text.contains("Salary"));
        verify(chartService).createSquareCategoryChart(expenseCategories, CategoryType.EXPENSE);
        verify(chartService).createSquareCategoryChart(incomeCategories, CategoryType.INCOME);
    }

    private PdfExportContext context(
            Boolean includeChart,
            List<ReportCategory> expenseCategories,
            List<ReportCategory> incomeCategories
    ) {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(9);
        request.setYear(2026);
        request.setReportType(ReportType.CATEGORY);
        request.setIncludeChart(includeChart);
        request.setIncludeTopExpenses(false);

        return new PdfExportContext(
                request,
                null,
                List.of(),
                new CategoryReportData(expenseCategories, incomeCategories)
        );
    }

    private byte[] renderPdf(PdfExportContext context) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        PdfWriter.getInstance(document, outputStream);
        document.open();
        renderer.render(document, context);
        document.close();

        return outputStream.toByteArray();
    }

    private String extractText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();

            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page));
            }

            return text.toString();
        }
    }

    private void assertValidPdf(byte[] pdf) {
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    private ReportCategory category(
            String name,
            String amount,
            String percentage
    ) {
        return new ReportCategory(
                name,
                new BigDecimal(amount),
                new BigDecimal(percentage)
        );
    }

    private Image onePixelImage() throws Exception {
        return Image.getInstance(
                1,
                1,
                3,
                8,
                new byte[]{0, 0, 0}
        );
    }
}
