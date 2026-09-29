package org.example.pft.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardData {
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private List<PieChartData> pieChart;
    private List<RecentTransData> recentTransactions;
}
