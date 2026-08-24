package org.example.pft.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String type;

    private String emoji;
}
