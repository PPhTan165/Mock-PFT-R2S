package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.ReportService;
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
class MonthlyPdfRendererTest {

    @Mock
    ChartService chartService;

    @Mock
    ReportService reportService;

    MonthlyPdfRenderer renderer;

    @BeforeEach
    void setup() {
        renderer = new MonthlyPdfRenderer(
                new PdfReportHelper(),
                chartService,
                reportService
        );
    }

    @Test
    void render_withValidMonthlyDataAndChartDisabled_shouldWriteComparisonTable() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(9, 2026, false, current, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang so sanh 2 thang truoc"));
        assertTrue(text.contains("Thang"));
        assertTrue(text.contains("Income"));
        assertTrue(text.contains("Expenses"));
        assertTrue(text.contains("Balance"));
        assertTrue(text.contains("August 2026"));
        assertTrue(text.contains("$2,100.00"));
        assertTrue(text.contains("$700.00"));
        assertTrue(text.contains("$1,400.00"));
        assertTrue(text.contains("September 2026"));
        assertTrue(text.contains("$2,500.00"));
        assertTrue(text.contains("$875.50"));
        assertTrue(text.contains("$1,624.50"));
        verify(chartService, never())
                .createMonthlyLineChart(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void render_withChartEnabledAndGeneratedChart_shouldRequestChartAndRenderPdf() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");
        List<ChartData> chartData = chartData();
        Image chart = Image.getInstance(
                1,
                1,
                3,
                8,
                new byte[]{0, 0, 0}
        );

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));
        when(chartService.createMonthlyLineChart(chartData, 2026))
                .thenReturn(chart);

        byte[] pdf = renderPdf(context(9, 2026, true, current, chartData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("September 2026"));
        assertTrue(text.contains("$2,500.00"));
        verify(chartService).createMonthlyLineChart(chartData, 2026);
    }

    @Test
    void render_withChartEnabledAndNullChart_shouldKeepValidTablePdf() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");
        List<ChartData> chartData = chartData();

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));
        when(chartService.createMonthlyLineChart(chartData, 2026))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(9, 2026, true, current, chartData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Bang so sanh 2 thang truoc"));
        assertTrue(text.contains("September 2026"));
        verify(chartService).createMonthlyLineChart(chartData, 2026);
    }

    @Test
    void render_withMissingCurrentMonthData_shouldWriteFallbackRowValues() throws Exception {
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(null));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(9, 2026, false, null, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("August 2026"));
        assertTrue(text.contains("N/A"));
        assertTrue(countOccurrences(text, "$0.00") >= 3);
        verify(chartService, never())
                .createMonthlyLineChart(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void render_withMissingPreviousMonthData_shouldWriteFallbackPreviousRow() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(null));

        byte[] pdf = renderPdf(context(9, 2026, false, current, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("N/A"));
        assertTrue(text.contains("September 2026"));
        assertTrue(text.contains("$2,500.00"));
        assertTrue(countOccurrences(text, "$0.00") >= 3);
    }

    @Test
    void render_forJanuary_shouldCompareWithPreviousDecember() throws Exception {
        SummaryData current = summary("January", 2026, "3100.00", "1200.00", "1900.00");
        SummaryData previous = summary("December", 2025, "2900.00", "1100.00", "1800.00");

        when(reportService.showSummary(1, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(12, 2025))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(1, 2026, false, current, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("December 2025"));
        assertTrue(text.contains("$2,900.00"));
        assertTrue(text.contains("January 2026"));
        assertTrue(text.contains("$3,100.00"));
        verify(reportService).showSummary(1, 2026);
        verify(reportService).showSummary(12, 2025);
    }

    @Test
    void render_withChartEnabledButNullContextSummary_shouldSkipChart() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(9, 2026, true, null, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("September 2026"));
        verify(chartService, never())
                .createMonthlyLineChart(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void render_withChartEnabledButEmptyContextSummary_shouldSkipChart() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");
        SummaryData emptyContextSummary = summary("September", 2026, null, null, null, null);

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(9, 2026, true, emptyContextSummary, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("September 2026"));
        verify(chartService, never())
                .createMonthlyLineChart(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void render_withChartEnabledButZeroSummaryWithEmptyTopExpenses_shouldSkipChart() throws Exception {
        SummaryData current = summary("September", 2026, "2500.00", "875.50", "1624.50");
        SummaryData previous = summary("August", 2026, "2100.00", "700.00", "1400.00");
        SummaryData emptyContextSummary = summary("September", 2026, "0.00", "0.00", "0.00");

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));

        byte[] pdf = renderPdf(context(9, 2026, true, emptyContextSummary, chartData()));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("September 2026"));
        verify(chartService, never())
                .createMonthlyLineChart(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void render_withChartEnabledAndZeroSummaryWithTopExpenses_shouldStillRenderChart() throws Exception {
        SummaryData current = summary("September", 2026, "0.00", "0.00", "0.00");
        SummaryData previous = summary("August", 2026, "0.00", "0.00", "0.00");
        SummaryData summaryWithTopExpenses = summary(
                "September",
                2026,
                "0.00",
                "0.00",
                "0.00",
                List.of(new TopExpenses("Food", null, null, new BigDecimal("15.00")))
        );
        List<ChartData> chartData = chartData();

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));
        when(chartService.createMonthlyLineChart(chartData, 2026))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(9, 2026, true, summaryWithTopExpenses, chartData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("September 2026"));
        verify(chartService).createMonthlyLineChart(chartData, 2026);
    }

    @Test
    void render_withChartEnabledAndExpenseOnlySummary_shouldRenderChart() throws Exception {
        SummaryData current = summary("September", 2026, "0.00", "875.50", "-875.50");
        SummaryData previous = summary("August", 2026, "0.00", "700.00", "-700.00");
        List<ChartData> chartData = chartData();

        when(reportService.showSummary(9, 2026))
                .thenReturn(response(current));
        when(reportService.showSummary(8, 2026))
                .thenReturn(response(previous));
        when(chartService.createMonthlyLineChart(chartData, 2026))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(9, 2026, true, current, chartData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("-$875.50"));
        verify(chartService).createMonthlyLineChart(chartData, 2026);
    }

    private PdfExportContext context(
            Integer month,
            Integer year,
            Boolean includeChart,
            SummaryData summaryData,
            List<ChartData> chartData
    ) {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(month);
        request.setYear(year);
        request.setReportType(ReportType.MONTHLY);
        request.setIncludeChart(includeChart);
        request.setIncludeTopExpenses(false);

        return new PdfExportContext(
                request,
                summaryData,
                chartData,
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

    private ReportResponse<SummaryData> response(SummaryData data) {
        ReportResponse<SummaryData> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setData(data);
        return response;
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

    private BigDecimal amount(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private List<ChartData> chartData() {
        return List.of(
                new ChartData("Aug", new BigDecimal("2100.00"), new BigDecimal("700.00")),
                new ChartData("Sep", new BigDecimal("2500.00"), new BigDecimal("875.50"))
        );
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
