package org.example.pft.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryByTypeData {
    private Long id;
    private String name;
    private String icon;
    private String emojiUrl;
}
