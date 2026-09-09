package org.example.pft.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.entity.User;
import org.example.pft.enums.ReportType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.PdfExportService;
import org.example.pft.service.ReportService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {
    private final ReportService reportService;
    private final CurrentUserHelper currentUserHelper;
    private final PdfReportHelper pdfReportHelper;

    // Generate a local PDF report from the validated request.
    @Override
    public ReportResponse<String> exportPDF(PdfExportRequest request) {
        ReportType reportType = request.getReportType() == null ? ReportType.SUMMARY : request.getReportType();
        if (reportType != ReportType.SUMMARY) {
            throw new BusinessValidationException("Only SUMMARY PDF export is supported now");
        }

        SummaryData data = reportService.showSummary(request.getMonth(), request.getYear()).getData();
        System.out.println(data);
        Path filePath = createFilePath(request);
        writeSummaryPdf(filePath, request, data);

        ReportResponse<String> response = new ReportResponse<>();
        response.setSuccess(true);
        response.setMessage("Summary report PDF generated successfully");
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

        // Get the authenticated user so the file name belongs to the current account.
        User user = currentUserHelper.getCurrentUser();

        // Build a stable file name, for example: 123_summary_apr_2024.pdf.
        String fileName = String.format(
                "%d_summary_%s_%d.pdf",
                user.getId(),
                monthShortName(request.getMonth()).toLowerCase(Locale.ENGLISH),
                request.getYear()
        );

        // Return the final local path where the PDF will be saved.
        return Path.of("reports", fileName);
    }

    private void writeSummaryPdf(Path filePath, PdfExportRequest request, SummaryData data) {
        // Create an A4 PDF document instance before binding it to the output file.
        Document document = new Document(PageSize.A4);
        OutputStream outputStream = null;

        try {
            // Open a file output stream to write the generated PDF bytes to the target path.
            outputStream = Files.newOutputStream(filePath);

            // Connect OpenPDF's writer to the document and the local file stream.
            PdfWriter.getInstance(document, outputStream);

            // Open the document before adding paragraphs, tables, or other PDF content.
            document.open();

            // Add the main report title with the selected month and year.
            pdfReportHelper.addTitle(document, "Summary Report - " + monthFullName(request.getMonth()) + " " + request.getYear());

            // If there is no summary data, keep generating the PDF and show a no-data message.
            if (isEmptySummary(data)) {
                pdfReportHelper.addText(document, "No financial data found for " + monthFullName(request.getMonth()) + " " + request.getYear());
            }

            // Add the summary section title before rendering income, expense, balance, and budget status.
            pdfReportHelper.addSectionTitle(document, "Thong tin tong hop thang");

            // Render the required monthly summary table.
            writeSummaryTable(document, data);

            // Render Top 3 expenses only when the client explicitly includes this section.
            if (Boolean.TRUE.equals(request.getIncludeTopExpenses())) {
                pdfReportHelper.addSectionTitle(document, "Top 3 khoan chi tieu");
                writeTopExpensesTable(document, data);
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
