package org.example.pft.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PieChartData {
    private String  category;
    private BigDecimal amount;
    private CategoryType type;
}
