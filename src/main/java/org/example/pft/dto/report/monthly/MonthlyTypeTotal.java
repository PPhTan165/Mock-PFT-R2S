package org.example.pft.dto.report.monthly;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyTypeTotal {
    private Integer month;
    private CategoryType type;
    private BigDecimal total;
}
