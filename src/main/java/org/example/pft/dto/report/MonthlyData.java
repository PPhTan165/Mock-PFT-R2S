package org.example.pft.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyData {
    private List<ChartData> chart;
    private SummaryData summary;
}
