package org.example.pft.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Category type must be INCOME or EXPENSE")
    @Pattern(regexp = "(?i)INCOME|EXPENSE")
    private String type;

    private String emoji;
}
