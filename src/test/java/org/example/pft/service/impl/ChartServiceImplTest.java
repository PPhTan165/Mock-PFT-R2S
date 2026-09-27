package org.example.pft.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfWriter;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartServiceImplTest {

    ChartServiceImpl chartService;

    @BeforeEach
    void setup() {
        chartService = new ChartServiceImpl();
    }

    @Test
    void createIncomeExpenseChart_withValidData_shouldReturnEmbeddableImage() throws Exception {
        SummaryData summaryData = new SummaryData(
                "September",
                (short) 2026,
                new BigDecimal("10000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("-5000.00"),
                List.of()
        );

        Image image = chartService.createIncomeExpenseChart(summaryData);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createIncomeExpenseChart_withNullAmounts_shouldTreatAmountsAsZero() throws Exception {
        SummaryData summaryData = new SummaryData(
                "September",
                (short) 2026,
                null,
                null,
                null,
                List.of()
        );

        Image image = chartService.createIncomeExpenseChart(summaryData);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createIncomeExpenseChart_withNullData_shouldReturnNull() {
        assertNull(chartService.createIncomeExpenseChart(null));
    }

    @Test
    void createTopExpensesChart_withValidExpenses_shouldReturnEmbeddableImage() throws Exception {
        List<TopExpenses> topExpenses = List.of(
                topExpense("Food", "300.00"),
                topExpense("Transportation", "125.50"),
                topExpense("Entertainment", "74.50")
        );

        Image image = chartService.createTopExpensesChart(topExpenses);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createTopExpensesChart_withNullAmount_shouldTreatAmountAsZero() throws Exception {
        List<TopExpenses> topExpenses = List.of(
                new TopExpenses("Food", null, null, null)
        );

        Image image = chartService.createTopExpensesChart(topExpenses);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createTopExpensesChart_withNullOrEmptyInput_shouldReturnNull() {
        assertNull(chartService.createTopExpensesChart(null));
        assertNull(chartService.createTopExpensesChart(List.of()));
    }

    @Test
    void createMonthlyLineChart_withCompleteYearData_shouldReturnEmbeddableImage() throws Exception {
        List<ChartData> chartData = List.of(
                chartData("Jan", "1000.00", "700.00"),
                chartData("Feb", "1100.00", "750.00"),
                chartData("Mar", "1200.00", "800.00"),
                chartData("Apr", "1300.00", "850.00"),
                chartData("May", "1400.00", "900.00"),
                chartData("Jun", "1500.00", "950.00"),
                chartData("Jul", "1600.00", "1000.00"),
                chartData("Aug", "1700.00", "1050.00"),
                chartData("Sep", "1800.00", "1100.00"),
                chartData("Oct", "1900.00", "1150.00"),
                chartData("Nov", "2000.00", "1200.00"),
                chartData("Dec", "2100.00", "1250.00")
        );

        Image image = chartService.createMonthlyLineChart(chartData, 2026);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createMonthlyLineChart_withNullAmounts_shouldTreatAmountsAsZero() throws Exception {
        List<ChartData> chartData = List.of(
                new ChartData("Sep", null, null)
        );

        Image image = chartService.createMonthlyLineChart(chartData, 2026);

        assertEmbeddableImage(image, 480f, 200f);
    }

    @Test
    void createMonthlyLineChart_withNullOrEmptyInput_shouldReturnNull() {
        assertNull(chartService.createMonthlyLineChart(null, 2026));
        assertNull(chartService.createMonthlyLineChart(List.of(), 2026));
    }

    @Test
    void createSquareCategoryChart_withExpenseCategories_shouldReturnEmbeddableImage() throws Exception {
        List<ReportCategory> categories = List.of(
                category("Food", "300.00", "60.00"),
                category("Transportation", "125.50", "25.10"),
                category("Entertainment", "74.50", "14.90")
        );

        Image image = chartService.createSquareCategoryChart(categories, CategoryType.EXPENSE);

        assertEmbeddableImage(image, 600f, 300f);
    }

    @Test
    void createSquareCategoryChart_withIncomeCategoriesIncludingZeroValue_shouldReturnEmbeddableImage() throws Exception {
        List<ReportCategory> categories = List.of(
                category("Salary", "2500.00", "100.00"),
                category("Freelance", "0.00", "0.00")
        );

        Image image = chartService.createSquareCategoryChart(categories, CategoryType.INCOME);

        assertEmbeddableImage(image, 600f, 300f);
    }

    @Test
    void createSquareCategoryChart_withNullOrEmptyInput_shouldReturnNull() {
        assertNull(chartService.createSquareCategoryChart(null, CategoryType.EXPENSE));
        assertNull(chartService.createSquareCategoryChart(List.of(), CategoryType.INCOME));
    }

    private TopExpenses topExpense(
            String category,
            String amount
    ) {
        return new TopExpenses(category, null, null, new BigDecimal(amount));
    }

    private ChartData chartData(
            String month,
            String income,
            String expense
    ) {
        return new ChartData(
                month,
                new BigDecimal(income),
                new BigDecimal(expense)
        );
    }

    private ReportCategory category(
            String category,
            String amount,
            String percentage
    ) {
        return new ReportCategory(
                category,
                new BigDecimal(amount),
                new BigDecimal(percentage)
        );
    }

    private void assertEmbeddableImage(
            Image image,
            float maxWidth,
            float maxHeight
    ) throws Exception {
        assertNotNull(image);
        assertTrue(image.getScaledWidth() > 0);
        assertTrue(image.getScaledHeight() > 0);
        assertTrue(image.getScaledWidth() <= maxWidth);
        assertTrue(image.getScaledHeight() <= maxHeight);
        assertEquals(12f, image.getSpacingBefore());
        assertEquals(12f, image.getSpacingAfter());

        byte[] pdf = embedInPdf(image);
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    private byte[] embedInPdf(Image image) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        PdfWriter.getInstance(document, outputStream);
        document.open();
        document.add(image);
        document.close();

        return outputStream.toByteArray();
    }
}
