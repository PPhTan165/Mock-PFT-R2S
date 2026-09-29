package org.example.pft.service.pdf;

import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.pdf.PdfExportRequest;
import org.example.pft.dto.report.summary.SummaryData;

import java.util.List;

public record PdfExportContext(
        PdfExportRequest request,
        SummaryData summaryData,
        List<ChartData> chartData,
        CategoryReportData categoryReportData
) {
}
