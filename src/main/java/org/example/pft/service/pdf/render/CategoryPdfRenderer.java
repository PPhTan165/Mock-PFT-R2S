package org.example.pft.service.pdf.render;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPTable;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.enums.CategoryType;
import org.example.pft.enums.ReportType;
import org.example.pft.helper.PdfReportHelper;
import org.example.pft.service.ChartService;
import org.example.pft.service.impl.PdfExportServiceImpl;
import org.example.pft.service.pdf.CategoryReportData;
import org.example.pft.service.pdf.PdfExportContext;
import org.example.pft.service.pdf.PdfReportRenderer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@AllArgsConstructor
public class CategoryPdfRenderer implements PdfReportRenderer {
    private final PdfReportHelper pdfReportHelper;
    private final ChartService chartService;

    @Override
    public ReportType supportType() {
        return ReportType.CATEGORY;
    }

    @Override
    public String title() {
        return "Category Report";
    }

    @Override
    public void render(Document document, PdfExportContext context) throws DocumentException {
        writeCategorySection(document, CategoryType.EXPENSE, context.categoryReportData().expenseCategories());
        writeCategorySection(document, CategoryType.INCOME, context.categoryReportData().incomeCategories());
        if (Boolean.TRUE.equals(context.request().getIncludeChart())) {
            writeCategoryPieChart(document, context.categoryReportData());
        }
    }

    private void writeCategorySection(Document document, CategoryType type, List<ReportCategory> categories)
            throws DocumentException {
        pdfReportHelper.addSectionTitle(document, "Bang thu/chi: " + type);

        if (categories == null || categories.isEmpty()) {
            pdfReportHelper.addText(document, "No " + type.toString().toLowerCase(Locale.ENGLISH) + " category data available");
            return;
        }

        PdfPTable table = pdfReportHelper.createTable(3, 3f, 2f, 2f);
        pdfReportHelper.addHeader(table, "Category", "Amount", "Percentage");

        for (ReportCategory item : categories) {
            pdfReportHelper.addCell(table, item.getCategory());
            pdfReportHelper.addCell(table, pdfReportHelper.formatAmount(item.getAmount()));
            pdfReportHelper.addCell(table, pdfReportHelper.formatPercentage(item.getPercentage()));
        }

        document.add(table);
    }

    private void writeCategoryPieChart(Document document, CategoryReportData categoryReportData) throws DocumentException {
        Image expenseCategoryPieChart = chartService.createSquareCategoryChart(
                categoryReportData.expenseCategories(),
                CategoryType.EXPENSE
        );
        Image incomeCategoryPieChart = chartService.createSquareCategoryChart(
                categoryReportData.incomeCategories(),
                CategoryType.INCOME
        );
        addChart(document,expenseCategoryPieChart);
        addChart(document,incomeCategoryPieChart);
    }

    private void addChart(Document document, Image chart) throws DocumentException {
        if (chart != null) {
            document.add(chart);
        }
    }


}
