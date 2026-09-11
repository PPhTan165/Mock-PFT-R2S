package org.example.pft.service;

import com.lowagie.text.Image;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;

import java.util.List;

public interface ChartService {

    Image createIncomeExpenseChart(
            SummaryData data
    );

    Image createTopExpensesChart(
            List<TopExpenses> topExpenses
    );

    Image createMonthlyLineChart(List<ChartData> chartData, Integer year);
}
