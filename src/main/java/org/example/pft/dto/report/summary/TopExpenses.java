package org.example.pft.dto.report.summary;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TopExpenses {
    private String category;
    private String icon;
    private String iconUrl;
    private BigDecimal amount;
    private BigDecimal percentage;

    public TopExpenses(String category, String icon, String iconUrl, BigDecimal amount) {
        this.category = category;
        this.icon = icon;
        this.iconUrl = iconUrl;
        this.amount = amount;
    }
}
