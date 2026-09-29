package org.example.pft.service.impl;

import lombok.AllArgsConstructor;
import org.example.pft.dto.dashboard.DashboardData;
import org.example.pft.dto.dashboard.PieChartData;
import org.example.pft.dto.dashboard.RecentTransData;
import org.example.pft.dto.dashboard.DashboardResponse;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final TransactionRepository transactionRepository;
    private final CurrentUserHelper currentUserHelper;

    private DashboardData mapToResponse(Integer month, Integer year){
        User user = currentUserHelper.getCurrentUser();
        Long userId = user.getId();

        BigDecimal income = getTotalByType(userId, month, year, CategoryType.INCOME);
        BigDecimal expense = getTotalByType(userId, month, year, CategoryType.EXPENSE);

        BigDecimal balance = income.subtract(expense);

        List<PieChartData> pieChart = transactionRepository.findPieChartData(userId, month, year);

        List<RecentTransData> recentTransactions = transactionRepository.findRecentTransData(
                userId,
                month,
                year,
                PageRequest.of(0, 3)
        );

        return new DashboardData(
                income,
                expense,
                balance,
                pieChart,
                recentTransactions
        );
    }

    private BigDecimal getTotalByType(Long userId, Integer month, Integer year, CategoryType type) {
        BigDecimal total = transactionRepository.getTotalByType(userId, month, year, type);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public DashboardResponse showDashboard(Integer month, Integer year){
        DashboardResponse response = new DashboardResponse();
        response.setSuccess(true);
        response.setMessage("Dashboard data fetched successfully");
        response.setData(mapToResponse(month, year));

        return response;
    }
}
