package org.example.pft.dto.report.category;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"category","amount","percentage"})
public class ReportCategory {
    private String category;
    private BigDecimal amount;
    private BigDecimal percentage;

    public ReportCategory(String category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}
