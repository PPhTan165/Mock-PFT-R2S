package org.example.pft.dto.transaction;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({"id","category","amount","date"})
public class HistoryData {
    private Long id;
    private HistoryCategory category;
    private BigDecimal amount;
    private LocalDate date;

    public HistoryData(
            Long id,
            String categoryName,
            String icon,
            CategoryType type,
            BigDecimal amount,
            LocalDate date
    ) {
        this.id = id;
        this.category = new HistoryCategory(categoryName, icon, type);
        this.amount = amount;
        this.date = date;
    }
}
