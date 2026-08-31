package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.report.ReportCategory;
import org.example.pft.dto.report.ReportCategoryData;
import org.example.pft.dto.report.ReportResponse;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final CategoryRepository categoryRepository;
    private final CurrentUserHelper currentUserHelper;

    @Override
    public ReportResponse showReportCategory(Integer month, Integer year, CategoryType type) {
        ReportResponse response = new ReportResponse();
        response.setSuccess(true);
        response.setMessage("Category breakdown fetched successfully");
        response.setData(mapToData(month, year, type));

        return response;
    }

    private BigDecimal getPercentage(BigDecimal amount, BigDecimal total) {
        if (total == null || amount == null
                || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return amount
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private ReportCategoryData mapToData(Integer month, Integer year, CategoryType type) {
        User user = currentUserHelper.getCurrentUser();
        Long userId = user.getId();

        List<ReportCategory> reportCategories =
                categoryRepository.findReportCategoryData(type, userId, month, year);

        BigDecimal total = reportCategories.stream()
                .map(ReportCategory::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (ReportCategory rc : reportCategories) {
            rc.setPercentage(getPercentage(rc.getAmount(), total));
        }

        return new ReportCategoryData(
                type,
                total,
                reportCategories
        );
    }
}
