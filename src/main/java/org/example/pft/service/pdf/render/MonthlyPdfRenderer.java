package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPTable;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.ReportService;
import org.example.pft.service.pdf.PdfExportContext;
import org.example.pft.service.pdf.PdfReportRenderer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Component
@AllArgsConstructor
public class MonthlyPdfRenderer implements PdfReportRenderer {
    private final PdfReportHelper pdfReportHelper;
    private final ChartService chartService;
    private final ReportService reportService;

    @Override
    public ReportType supportType() {
        return ReportType.MONTHLY;
    }

    @Override
    public String title() {
        return "Monthly Report";
    }

    @Override
    public void render(Document document, PdfExportContext context) throws DocumentException {
        pdfReportHelper.addSectionTitle(document, "Bang so sanh 2 thang truoc");
        writeMonthlyTable(document, context.request().getMonth(), context.request().getYear());

        if (Boolean.TRUE.equals(context.request().getIncludeChart()) && !isEmptySummary(context.summaryData())) {
            writeMonthlyLineChart(document, context.request(), context.chartData());
        }
    }

    private void writeMonthlyTable(Document document, Integer month, Integer year) throws DocumentException {
        SummaryData currentMonth = reportService.showSummary(month, year).getData();
        YearMonth previousYearMonth = YearMonth.of(year, month).minusMonths(1);
        SummaryData previousMonth = reportService
                .showSummary(previousYearMonth.getMonthValue(), previousYearMonth.getYear())
                .getData();

        PdfPTable table = pdfReportHelper.createTable(4, 3, 2, 2, 3);
        pdfReportHelper.addHeader(table, "Thang", "Income", "Expenses", "Balance");

        addMonthlyRow(table, previousMonth);
        addMonthlyRow(table, currentMonth);

        document.add(table);
    }

    private void writeMonthlyLineChart(Document document, PdfExportRequest request, List<ChartData> data) throws DocumentException {
        Image monthlyLineChart = chartService.createMonthlyLineChart(data, request.getYear());
        addChart(document, monthlyLineChart);
    }

    private void addMonthlyRow(PdfPTable table, SummaryData data) {
        pdfReportHelper.addCell(table, data == null ? "N/A" : data.getMonth() + " " + data.getYear());
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getIncome()));
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getExpense()));
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getBalance()));
    }

    private boolean isEmptySummary(SummaryData data) {
        return data == null
                || amount(data.getIncome()).compareTo(BigDecimal.ZERO) == 0
                && amount(data.getExpense()).compareTo(BigDecimal.ZERO) == 0
                && (data.getTopExpenses() == null || data.getTopExpenses().isEmpty());
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void addChart(Document document, Image chart) throws DocumentException {
        if (chart != null) {
            document.add(chart);
        }
    }

}
