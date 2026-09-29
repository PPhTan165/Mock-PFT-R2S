package org.example.pft.dto.budget;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBudgetRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Min(value = 1, message = "Month must be in range from 1-12")
    @Max(value = 12, message = "Month must be in range from 1-12")
    private Integer month;

    @NotNull(message = "Year is required")
    private Integer year;
}
