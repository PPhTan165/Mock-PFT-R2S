package org.example.pft.dto.budget;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"id","category","amount","month","year"})
public class BudgetData {
    private Long id;
    private BudgetCategory category;
    private BigDecimal amount;
    private Integer month;
    private Integer year;
    private CategoryType type;

}
