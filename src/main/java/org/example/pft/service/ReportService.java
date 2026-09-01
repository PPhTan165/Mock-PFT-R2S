package org.example.pft.service;

import org.example.pft.dto.report.MonthlyData;
import org.example.pft.dto.report.ReportCategoryData;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.enums.CategoryType;

public interface ReportService {
        ReportResponse<ReportCategoryData> showReportCategory(Integer month, Integer year, CategoryType type);
        ReportResponse<MonthlyData> showMonthly(Integer month, Integer year);
}
