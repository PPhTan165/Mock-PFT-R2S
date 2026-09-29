package org.example.pft.dto.budget;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"success","message","data"})
public class BudgetResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
