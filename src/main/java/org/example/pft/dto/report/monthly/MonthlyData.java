package org.example.pft.dto.report.monthly;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyData {
    private List<ChartData> chart;
    private SummaryMonthlyData summary;
}
