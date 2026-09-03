package org.example.pft.dto.report.monthly;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SummaryData {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
}
