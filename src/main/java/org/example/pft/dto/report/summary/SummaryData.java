package org.example.pft.dto.report.summary;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class SummaryData {
    private String month;
    private Short year;
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
    private List<TopExpenses> topExpenses;
}
