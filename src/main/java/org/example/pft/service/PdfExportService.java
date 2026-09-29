package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.pdf.PdfExportRequest;

public interface PdfExportService {
    ReportResponse<String> exportPDF(PdfExportRequest request);
    byte[] generateSummaryPdf(PdfExportRequest request);
}
