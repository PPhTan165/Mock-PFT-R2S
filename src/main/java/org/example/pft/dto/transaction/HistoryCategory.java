package org.example.pft.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

@Data
@AllArgsConstructor

public class HistoryCategory {
    private String name;
    private String icon;
    private CategoryType type;
}
