package org.example.pft.dto.budget;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BudgetCategory {
    private Long id;
    private String name;
    private String icon;
    private String iconUrl;
}
