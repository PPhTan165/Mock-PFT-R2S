package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.enums.CategoryType;

public interface ReportService {
        ReportResponse showReportCategory(Integer month, Integer year, CategoryType type);
}
