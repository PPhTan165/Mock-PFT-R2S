package org.example.pft.dto.report.pdf;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Min(value = 1900, message = "Year must be between 1900 and 9999")
    @Max(value = 9999, message = "Year must be between 1900 and 9999")
    private Integer year;

    private Boolean includeChart;

    private Boolean includeTopExpenses;

    @Pattern(
            regexp = "^$|(?i:SUMMARY|MONTHLY|CATEGORY)",
            message = "Report type must be one of: SUMMARY, MONTHLY, CATEGORY"
    )
    private String reportType = ReportType.SUMMARY.name();

    public ReportType getReportType() {
        return parseReportType(reportType);
    }

    public void setReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            this.reportType = ReportType.SUMMARY.name();
            return;
        }

        this.reportType = reportType.trim();
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType == null ? ReportType.SUMMARY.name() : reportType.name();
    }

    private ReportType parseReportType(String reportType) {
        if (reportType == null || reportType.isBlank()) {
            return ReportType.SUMMARY;
        }

        return ReportType.valueOf(reportType.trim().toUpperCase(Locale.ENGLISH));
    }
}
