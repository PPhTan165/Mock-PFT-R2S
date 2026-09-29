package org.example.pft.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;

public interface PdfOptionalSectionRenderer {
    boolean supports(PdfExportContext context);

    void render(Document document,PdfExportContext context)
            throws DocumentException;
}
