package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.report.*;
import org.example.pft.dto.report.monthly.*;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.ReportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserHelper currentUserHelper;

    @Override
    public ReportResponse<ReportCategoryData> showReportCategory(Integer month, Integer year, CategoryType type) {
        ReportResponse response = new ReportResponse();
        response.setSuccess(true);
        response.setMessage("Category breakdown fetched successfully");
        response.setData(mapToData(month, year, type));

        return response;
    }

    @Override
    public ReportResponse<MonthlyData> showMonthly(Integer month, Integer year){
        ReportResponse response = new ReportResponse();
        response.setSuccess(true);
        response.setMessage("Monthly financial report fetched successfully");
        response.setData(mapToMonthlyData(month,year));

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

    private BigDecimal getTotalByType(Long userId, Integer month, Integer year, CategoryType type) {
        BigDecimal total = transactionRepository.getTotalByType(userId, month, year, type);
        return total == null ? BigDecimal.ZERO : total;
    }

    private String parseMonthToString(Integer month){
        return Month.of(month)
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                .toUpperCase();
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

    private MonthlyData mapToMonthlyData(Integer month, Integer year){
        User user = currentUserHelper.getCurrentUser();
        Long userId = user.getId();

        BigDecimal incomeByMonth = getTotalByType(userId,month,year,CategoryType.INCOME);
        BigDecimal expenseByMonth = getTotalByType(userId,month,year,CategoryType.EXPENSE);

        String monthName = Month.of(month)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        SummaryData summary = new SummaryData(
                monthName + ' ' +year,
                incomeByMonth,
                expenseByMonth
        );

        List<ChartData> charts = new ArrayList<>();
        for(int i = 1; i<= 12; i++){
            String monthChart = parseMonthToString(i);
            BigDecimal incomeOfChart = getTotalByType(userId,i,year,CategoryType.INCOME);
            BigDecimal expenseOfChart = getTotalByType(userId,i,year,CategoryType.EXPENSE);

            ChartData chart = new ChartData(monthChart,incomeOfChart,expenseOfChart);
            charts.add(chart);
        }

        MonthlyData data = new MonthlyData(
                charts,
                summary
        );

        return data;

    }
}
