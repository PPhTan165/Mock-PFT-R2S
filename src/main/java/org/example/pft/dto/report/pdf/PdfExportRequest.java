package org.example.pft.dto.report.pdf;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.pft.enums.ReportType;

import java.util.Locale;

@Data
public class PdfExportRequest {

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;

    private Boolean includeChart;

    private Boolean includeTopExpenses;

    private ReportType reportType = ReportType.SUMMARY;

    public void setReportType(ReportType reportType) {
        this.reportType = reportType == null ? ReportType.SUMMARY : reportType;
    }

    public void setReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            this.reportType = ReportType.SUMMARY;
            return;
        }

        this.reportType = ReportType.valueOf(reportType.trim().toUpperCase(Locale.ENGLISH));
    }
}
