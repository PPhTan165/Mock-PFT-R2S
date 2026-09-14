package org.example.pft.service.pdf;

import org.example.pft.dto.report.category.ReportCategory;

import java.util.List;

public record CategoryReportData(
        List<ReportCategory> expenseCategories,
        List<ReportCategory> incomeCategories
) {
}
