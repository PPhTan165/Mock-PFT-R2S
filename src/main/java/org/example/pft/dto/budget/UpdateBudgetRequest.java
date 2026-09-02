package org.example.pft.dto.budget;

import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UpdateBudgetRequest {
    @Min(value = 1,message = "Amount must be greater than 0")
    private BigDecimal amount;
}
