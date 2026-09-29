package org.example.pft.service.pdf.section;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPTable;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.pdf.PdfExportContext;
import org.example.pft.service.pdf.PdfOptionalSectionRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class TopExpensesPdfSectionRenderer implements PdfOptionalSectionRenderer {
    private final PdfReportHelper pdfReportHelper;
    private ChartService chartService;

    @Override
    public boolean supports(PdfExportContext context) {
        return Boolean.TRUE.equals(context.request().getIncludeTopExpenses())
                && context.summaryData() != null
                && context.summaryData().getTopExpenses() != null
                && !context.summaryData().getTopExpenses().isEmpty();
    }

    @Override
    public void render(Document document, PdfExportContext context) throws DocumentException {
        pdfReportHelper.addSectionTitle(document, "Top 3 khoan chi tieu");
        writeTopExpensesTable(document, context.summaryData());

        if (Boolean.TRUE.equals(context.request().getIncludeChart())) {
            Image chart = chartService.createTopExpensesChart(
                    context.summaryData().getTopExpenses());

            addChart(document, chart);
        }
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

    private void addChart(Document document, Image chart) throws DocumentException {
        if (chart != null) {
            document.add(chart);
        }
    }

}
