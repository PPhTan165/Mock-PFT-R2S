package org.example.pft.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ChartData {
    private String month;
    private BigDecimal income;
    private BigDecimal expense;
}
