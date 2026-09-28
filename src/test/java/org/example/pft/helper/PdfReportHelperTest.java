package org.example.pft.helper;

import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfReportHelperTest {

    private final PdfReportHelper helper = new PdfReportHelper();

    @Test
    void addCell_withNullValue_shouldAddEmptyCell() throws Exception {
        PdfPTable table = helper.createTable(1, 1f);

        helper.addCell(table, null);

        PdfPCell cell = table.getRow(0).getCells()[0];
        assertNotNull(cell);
        assertNotNull(cell.getPhrase());
        assertEquals("", cell.getPhrase().getContent());
    }
}
