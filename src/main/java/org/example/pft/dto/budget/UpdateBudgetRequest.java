package org.example.pft.dto.budget;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateBudgetRequest {
    @NotNull(message = "Amount is required")
    @Min(value = 1,message = "Amount must be greater than 0")
    private BigDecimal amount;
}
