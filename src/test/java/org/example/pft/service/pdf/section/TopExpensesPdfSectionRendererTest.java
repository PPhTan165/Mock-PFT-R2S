package org.example.pft.service.pdf.section;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopExpensesPdfSectionRendererTest {

    @Mock
    ChartService chartService;

    TopExpensesPdfSectionRenderer renderer;

    @BeforeEach
    void setup() {
        renderer = new TopExpensesPdfSectionRenderer(
                new PdfReportHelper(),
                chartService
        );
    }

    @Test
    void supports_shouldRequireEnabledFlagAndNonEmptyTopExpenses() {
        assertTrue(renderer.supports(context(true, summaryWithTopExpenses())));

        assertFalse(renderer.supports(context(false, summaryWithTopExpenses())));
        assertFalse(renderer.supports(context(true, null)));
        assertFalse(renderer.supports(context(true, summaryWithTopExpenses(null))));
        assertFalse(renderer.supports(context(true, summaryWithTopExpenses(List.of()))));
    }

    @Test
    void render_withTopExpensesAndChartDisabled_shouldWriteTopExpensesTable() throws Exception {
        SummaryData summaryData = summaryWithTopExpenses(List.of(
                topExpense("Food", "120.50", "60.25"),
                topExpense("Transport", "79.50", "39.75")
        ));

        byte[] pdf = renderPdf(context(false, true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Top 3 khoan chi tieu"));
        assertTrue(text.contains("Category"));
        assertTrue(text.contains("Amount"));
        assertTrue(text.contains("Percentage"));
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("$120.50"));
        assertTrue(text.contains("60.25%"));
        assertTrue(text.contains("Transport"));
        assertTrue(text.contains("$79.50"));
        assertTrue(text.contains("39.75%"));
        verify(chartService, never())
                .createTopExpensesChart(summaryData.getTopExpenses());
    }

    @Test
    void render_withNullSummaryDataAndChartDisabled_shouldWriteFallbackMessage() throws Exception {
        byte[] pdf = renderPdf(context(false, true, null));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Top 3 khoan chi tieu"));
        assertTrue(text.contains("No top expenses available"));
        verify(chartService, never())
                .createTopExpensesChart(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void render_withNullTopExpensesAndChartDisabled_shouldWriteFallbackMessage() throws Exception {
        byte[] pdf = renderPdf(context(false, true, summaryWithTopExpenses(null)));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("No top expenses available"));
        verify(chartService, never())
                .createTopExpensesChart(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void render_withChartEnabledAndGeneratedChart_shouldRequestChartAndRenderPdf() throws Exception {
        SummaryData summaryData = summaryWithTopExpenses();
        Image chart = Image.getInstance(
                1,
                1,
                3,
                8,
                new byte[]{0, 0, 0}
        );

        when(chartService.createTopExpensesChart(summaryData.getTopExpenses()))
                .thenReturn(chart);

        byte[] pdf = renderPdf(context(true, true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("$120.50"));
        verify(chartService)
                .createTopExpensesChart(summaryData.getTopExpenses());
    }

    @Test
    void render_withChartEnabledAndNullChart_shouldRenderTableWithoutChart() throws Exception {
        SummaryData summaryData = summaryWithTopExpenses();

        when(chartService.createTopExpensesChart(summaryData.getTopExpenses()))
                .thenReturn(null);

        byte[] pdf = renderPdf(context(true, true, summaryData));
        String text = extractText(pdf);

        assertValidPdf(pdf);
        assertTrue(text.contains("Top 3 khoan chi tieu"));
        assertTrue(text.contains("Food"));
        assertTrue(text.contains("$120.50"));
        verify(chartService)
                .createTopExpensesChart(summaryData.getTopExpenses());
    }

    private PdfExportContext context(
            Boolean includeTopExpenses,
            SummaryData summaryData
    ) {
        return context(false, includeTopExpenses, summaryData);
    }

    private PdfExportContext context(
            Boolean includeChart,
            Boolean includeTopExpenses,
            SummaryData summaryData
    ) {
        PdfExportRequest request = new PdfExportRequest();
        request.setMonth(9);
        request.setYear(2026);
        request.setIncludeChart(includeChart);
        request.setIncludeTopExpenses(includeTopExpenses);

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

    private SummaryData summaryWithTopExpenses() {
        return summaryWithTopExpenses(List.of(
                topExpense("Food", "120.50", "60.25")
        ));
    }

    private SummaryData summaryWithTopExpenses(List<TopExpenses> topExpenses) {
        return new SummaryData(
                "September",
                (short) 2026,
                new BigDecimal("1000.00"),
                new BigDecimal("200.00"),
                new BigDecimal("800.00"),
                topExpenses
        );
    }

    private TopExpenses topExpense(
            String category,
            String amount,
            String percentage
    ) {
        TopExpenses topExpense = new TopExpenses(
                category,
                null,
                null,
                new BigDecimal(amount)
        );
        topExpense.setPercentage(new BigDecimal(percentage));
        return topExpense;
    }
}
