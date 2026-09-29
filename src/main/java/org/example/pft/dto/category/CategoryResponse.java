package org.example.pft.dto.category;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"success", "message", "data"})
public class CategoryResponse<T> {
    private boolean success = false;
    private String message;
    private T data;
}
