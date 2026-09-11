package org.example.pft.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.entity.User;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.PdfExportService;
import org.example.pft.service.ReportService;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {
    private final ReportService reportService;
    private final CurrentUserHelper currentUserHelper;
    private final PdfReportHelper pdfReportHelper;
    private final ChartService chartService;

    // Generate a local PDF report from the validated request.
    @Override
    public ReportResponse<String> exportPDF(PdfExportRequest request) {

        SummaryData data = reportService.showSummary(request.getMonth(), request.getYear()).getData();
        List<ChartData> chartData = reportService.showMonthly(request.getMonth(),request.getYear()).getData().getChart();
        Path filePath = createFilePath(request);
        writePdf(filePath, request, data, chartData);

        String type = request.getReportType().toString().toLowerCase();

        ReportResponse<String> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setMessage(type + " report PDF generated successfully");
        response.setData(filePath.toString());
        return response;
    }

    private Path createFilePath(PdfExportRequest request) {
        try {
            // Ensure the local reports folder exists before writing the PDF file.
            Files.createDirectories(Path.of("reports"));

        } catch (IOException ex) {
            // Stop the export when the application cannot prepare the output folder.
            throw new RuntimeException("Could not create reports directory", ex);
        }

        User user = currentUserHelper.getCurrentUser();

        String fileName = String.format(
                "%d_%s_%s_%d.pdf",
                user.getId(),
                request.getReportType().toString().toLowerCase(),
                monthShortName(request.getMonth()).toLowerCase(Locale.ENGLISH),
                request.getYear()
        );

        // Return the final local path where the PDF will be saved.
        return Path.of("reports", fileName);
    }

    private void writePdf(Path filePath, PdfExportRequest request, SummaryData data, List<ChartData> chartData) {
        Document document = new Document(PageSize.A4);
        OutputStream outputStream = null;

        try {
            outputStream = Files.newOutputStream(filePath);
            PdfWriter.getInstance(document, outputStream);

            document.open();

            pdfReportHelper.addTitle(document, "Summary Report - " + monthFullName(request.getMonth()) + " " + request.getYear());

            if (isEmptySummary(data)) {
                pdfReportHelper.addText(document, "No financial data found for " + monthFullName(request.getMonth()) + " " + request.getYear());
            }

            switch (request.getReportType()) {
                case SUMMARY -> {
                    pdfReportHelper.addSectionTitle(document, "Thong tin tong hop thang");
                    writeSummaryTable(document, data);
                    if (Boolean.TRUE.equals(request.getIncludeChart()) && !isEmptySummary(data)) {
                        writeSummaryCharts(document, request, data);
                    }
                }

                case MONTHLY -> {
                    pdfReportHelper.addSectionTitle(document, "So sanh thang truoc");
                    writeMonthlyTable(document, request.getMonth(), request.getYear());
                    if (Boolean.TRUE.equals(request.getIncludeChart()) && !isEmptySummary(data)) {
                        writeMonthlyLineChart(document, request, chartData);
                    }
                }

                case CATEGORY -> {
                    pdfReportHelper.addSectionTitle(document, "Chua hoan thien");

                }
            }

            // Render Top 3 expenses only when the client explicitly includes this section.
            if (Boolean.TRUE.equals(request.getIncludeTopExpenses())) {
                pdfReportHelper.addSectionTitle(document, "Top 3 khoan chi tieu");
                writeTopExpensesTable(document, data);
                if(Boolean.TRUE.equals(request.getIncludeChart())){
                    writeTopExpensesChart(document,request,data);
                }
            }

        } catch (DocumentException | IOException ex) {
            // Convert PDF/file writing failures into a runtime error for the existing exception flow.
            throw new RuntimeException("Could not generate PDF report", ex);
        } finally {
            // Close the document before closing the stream so OpenPDF can finish writing the file.
            if (document.isOpen()) {
                document.close();
            }

            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    throw new RuntimeException("Could not close PDF output stream", ex);
                }
            }
        }
    }

    private void writeSummaryTable(Document document, SummaryData data) throws DocumentException {
        PdfPTable table = pdfReportHelper.createTable(2, 2f, 3f);
        pdfReportHelper.addHeader(table, "Chi tieu", "Gia tri");

        pdfReportHelper.addCell(table, "Income");
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getIncome()));

        pdfReportHelper.addCell(table, "Expenses");
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getExpense()));

        pdfReportHelper.addCell(table, "Balance");
        pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(data == null ? null : data.getBalance()));

        pdfReportHelper.addCell(table, "Du/Thieu");
        pdfReportHelper.addCell(table, amount(data == null ? null : data.getBalance()).signum() >= 0 ? "Du ngan sach" : "Thieu ngan sach");

        document.add(table);
    }

    private void writeTopExpensesTable(Document document, SummaryData data) throws DocumentException {
        List<TopExpenses> topExpenses = data == null ? List.of() : data.getTopExpenses();
        if (topExpenses == null || topExpenses.isEmpty()) {
            pdfReportHelper.addText(document, "No top expenses available");
            return;
        }

        PdfPTable table = pdfReportHelper.createTable(3, 3f, 2f, 2f);
        pdfReportHelper.addHeader(table, "Category", "Amount", "Percentage");

        for (TopExpenses item : topExpenses) {
            pdfReportHelper.addCell(table, item.getCategory());
            pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(item.getAmount()));
            pdfReportHelper.addCell(table, pdfReportHelper.formatPercentage(item.getPercentage()));
        }

        document.add(table);
    }

    private void writeSummaryCharts(Document document, PdfExportRequest request, SummaryData data) throws DocumentException {
        pdfReportHelper.addSectionTitle(document, "Charts");

        Image incomeExpenseChart = chartService.createIncomeExpenseChart(data);
        addChart(document, incomeExpenseChart);


    }

    private void writeTopExpensesChart(Document document,PdfExportRequest request,SummaryData data){
        if (Boolean.TRUE.equals(request.getIncludeTopExpenses())) {
            Image topExpensesChart = chartService.createTopExpensesChart(data == null ? null : data.getTopExpenses());
            addChart(document, topExpensesChart);
        }
    }

    private void writeMonthlyLineChart(Document document, PdfExportRequest request, List<ChartData> data) throws DocumentException {
        Image monthlyLineChart = chartService.createMonthlyLineChart(data, request.getYear());
        addChart(document, monthlyLineChart);

    }

    private void addChart(Document document, Image chart) throws DocumentException {
        if (chart != null) {
            document.add(chart);
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

    private String monthShortName(Integer month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private String monthFullName(Integer month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
