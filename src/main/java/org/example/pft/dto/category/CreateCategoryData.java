package org.example.pft.dto.category;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.pft.entity.Category;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"id","name","type","icon","iconUrl"})
public class CreateCategoryData {
    private Long id;
    private String name;
    private String type;
    private String icon;
    private String iconUrl;
}
