package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPTable;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.pdf.PdfExportContext;
import org.example.pft.service.pdf.PdfReportRenderer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class SummaryPdfRenderer implements PdfReportRenderer {
    private final PdfReportHelper pdfReportHelper;
    private final ChartService chartService;


    @Override
    public ReportType supportType() {
        return ReportType.SUMMARY;
    }

    @Override
    public String title() {
        return "Summary Report";
    }

    @Override
    public void render(Document document, PdfExportContext context) throws DocumentException {
        SummaryData summaryData = context.summaryData();

        pdfReportHelper.addSectionTitle(document,"Bang thong tin tong hop thang");
        writeSummaryTable(document,summaryData);

        if(Boolean.TRUE.equals(context.request().getIncludeChart()) && !isEmptySummary(summaryData)){
            Image chart = chartService.createIncomeExpenseChart(summaryData);
            addChart(document,chart);
        }
    }

    private void writeSummaryTable(Document document, SummaryData data)
            throws DocumentException {
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
