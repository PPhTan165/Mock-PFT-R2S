package org.example.pft.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class RecentTransData {
    private Long id;
    private String category;
    private String icon;
    private BigDecimal amount;
    private LocalDate date;
    private CategoryType type;
}
