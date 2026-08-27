package org.example.pft.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.enums.CategoryType;

@Data
@AllArgsConstructor
public class TransactionCategoryData {

    private Long id;
    private String name;
    private CategoryType type;
    private String icon;
    private String iconUrl;
}
