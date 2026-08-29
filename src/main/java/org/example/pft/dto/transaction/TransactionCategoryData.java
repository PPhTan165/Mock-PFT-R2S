package org.example.pft.dto.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionCategoryData {

    private Long id;
    private String name;
    private String type;
    private String icon;
    private String iconUrl;
}
