package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SummaryPdfRendererTest {

    @Mock
    ChartService chartService;

    SummaryPdfRenderer renderer;

    @BeforeEach
    void setup() {
        renderer = new SummaryPdfRenderer(
                new PdfReportHelper(),
                chartService
        );
    }

    @Test
    void supportTypeAndTitle_shouldIdentifySummaryReport() {
        assertEquals(ReportType.SUMMARY, renderer.supportType());
        assertEquals("Summary Report", renderer.title());
    }

    @Test
    void render_withValidFinancialDataAndChartDisabled_shouldWriteSummaryTableWithoutChart() throws Exception {
        SummaryData summaryData = summary("September", 2026, "10000.00", "15000.00", "-5000.00");

        byte[] pdf = renderPdf(context(false, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thong tin tong hop thang"));
        assertTrue(text.contains("Income"));
        assertTrue(text.contains("$10,000.00"));
        assertTrue(text.contains("Expenses"));
        assertTrue(text.contains("$15,000.00"));
        assertTrue(text.contains("Balance"));
        assertTrue(text.contains("-$5,000.00"));
        assertTrue(text.contains("Du/Thieu"));
        assertTrue(text.contains("Thieu ngan sach"));
        verify(chartService, never()).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withChartEnabledAndGeneratedChart_shouldRequestChartAndRenderPdf() throws Exception {
        SummaryData summaryData = summary("September", 2026, "10000.00", "7000.00", "3000.00");
        Image chart = Image.getInstance(
                1,
                1,
                3,
                8,
                new byte[]{0, 0, 0}
        );

        when(chartService.createIncomeExpenseChart(summaryData))
                .thenReturn(chart);

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("$10,000.00"));
        assertTrue(text.contains("$7,000.00"));
        assertTrue(text.contains("$3,000.00"));
        assertTrue(text.contains("Du ngan sach"));
        verify(chartService).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withChartEnabledAndNullChart_shouldKeepValidSummaryPdf() throws Exception {
        SummaryData summaryData = summary("September", 2026, "10000.00", "15000.00", "-5000.00");

        when(chartService.createIncomeExpenseChart(summaryData))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thong tin tong hop thang"));
        assertTrue(text.contains("-$5,000.00"));
        assertTrue(text.contains("Thieu ngan sach"));
        verify(chartService).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withZeroFinancialDataAndChartEnabled_shouldWriteZeroValuesAndSkipChart() throws Exception {
        SummaryData summaryData = summary("September", 2026, "0.00", "0.00", "0.00");

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(countOccurrences(text, "$0.00") >= 3);
        assertTrue(text.contains("Du ngan sach"));
        verify(chartService, never()).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withZeroFinancialDataAndNullTopExpenses_shouldSkipChart() throws Exception {
        SummaryData summaryData = summary("September", 2026, "0.00", "0.00", "0.00", null);

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(countOccurrences(text, "$0.00") >= 3);
        assertTrue(text.contains("Du ngan sach"));
        verify(chartService, never()).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withNullSummaryDataAndChartEnabled_shouldWriteFallbackAmountsAndSkipChart() throws Exception {
        byte[] pdf = renderPdf(context(true, null));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang thong tin tong hop thang"));
        assertTrue(countOccurrences(text, "$0.00") >= 3);
        assertTrue(text.contains("Du ngan sach"));
        verify(chartService, never()).createIncomeExpenseChart(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void render_withExpenseOnlyDataAndChartEnabled_shouldTreatSummaryAsAvailable() throws Exception {
        SummaryData summaryData = summary("September", 2026, "0.00", "875.50", "-875.50");

        when(chartService.createIncomeExpenseChart(summaryData))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("$875.50"));
        assertTrue(text.contains("-$875.50"));
        assertTrue(text.contains("Thieu ngan sach"));
        verify(chartService).createIncomeExpenseChart(summaryData);
    }

    @Test
    void render_withZeroAmountsButTopExpensesPresent_shouldTreatSummaryAsAvailableForChart() throws Exception {
        SummaryData summaryData = summary(
                "September",
                2026,
                "0.00",
                "0.00",
                "0.00",
                List.of(topExpense("Food", "120.50"))
        );

        when(chartService.createIncomeExpenseChart(summaryData))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(countOccurrences(text, "$0.00") >= 3);
        assertTrue(text.contains("Du ngan sach"));
        verify(chartService).createIncomeExpenseChart(summaryData);
    }

    private PdfExportContext context(
            Boolean includeChart,
            SummaryData summaryData
    ) {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(9);
        request.setYear(2026);
        request.setReportType(ReportType.SUMMARY);
        request.setIncludeChart(includeChart);
        request.setIncludeTopExpenses(false);

        return new PdfExportContext(
                request,
                summaryData,
                List.of(),
                null
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

    private SummaryData summary(
            String month,
            int year,
            String income,
            String expense,
            String balance
    ) {
        return summary(month, year, income, expense, balance, List.of());
    }

    private SummaryData summary(
            String month,
            int year,
            String income,
            String expense,
            String balance,
            List<TopExpenses> topExpenses
    ) {
        return new SummaryData(
                month,
                (short) year,
                amount(income),
                amount(expense),
                amount(balance),
                topExpenses
        );
    }

    private TopExpenses topExpense(
            String category,
            String amount
    ) {
        return new TopExpenses(
                category,
                null,
                null,
                new BigDecimal(amount)
        );
    }

    private BigDecimal amount(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private int countOccurrences(String text, String expected) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(expected, index)) >= 0) {
            count++;
            index += expected.length();
        }

        return count;
    }
}
