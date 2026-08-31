package org.example.pft.dto.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportCategoryData {
    private CategoryType type;
    private BigDecimal total;
    private List<ReportCategory> categories;
}
