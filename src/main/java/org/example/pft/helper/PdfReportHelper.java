package org.example.pft.helper;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
public class PdfReportHelper {

    public PdfPTable createTable(int columns, float... widths) throws DocumentException {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100f);
        table.setWidths(widths);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(10f);
        return table;
    }

    public void addHeader(PdfPTable table, String... headers) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, Color.WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(new Color(41, 89, 166));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    public void addCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, bodyFont()));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    public void addTitle(Document document, String text) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingAfter(16f);
        document.add(paragraph);
    }

    public void addSectionTitle(Document document, String text) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
        paragraph.setSpacingBefore(8f);
        paragraph.setSpacingAfter(6f);
        document.add(paragraph);
    }

    public void addText(Document document, String text) throws DocumentException {
        Paragraph paragraph = new Paragraph(text, bodyFont());
        paragraph.setSpacingAfter(8f);
        document.add(paragraph);
    }

    public String formatAmount(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount(value));
    }

    public String formatPercentage(BigDecimal value) {
        return amount(value).setScale(2).toPlainString() + "%";
    }

    private Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 11);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
