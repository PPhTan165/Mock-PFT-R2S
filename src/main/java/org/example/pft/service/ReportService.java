package org.example.pft.service;

import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.enums.CategoryType;

public interface ReportService {
        ReportResponse<ReportCategoryData> showReportCategory(Integer month, Integer year, CategoryType type);
        ReportResponse<MonthlyData> showMonthly(Integer month, Integer year);
        ReportResponse<SummaryData> showSummary(Integer month, Integer year);
}
