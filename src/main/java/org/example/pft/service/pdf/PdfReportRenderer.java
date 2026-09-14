package org.example.pft.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import org.example.pft.enums.ReportType;

public interface PdfReportRenderer {
    ReportType supportType();

    String title();

    void render(Document document, PdfExportContext context)
            throws DocumentException;
}
