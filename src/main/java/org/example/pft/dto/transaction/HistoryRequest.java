package org.example.pft.dto.transaction;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.pft.enums.CategoryType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class HistoryRequest {
    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Long categoryId;

    @NotNull(message = "Type is required INCOME or EXPENSE")
    private CategoryType type;

    @Min(value = 1, message = "Page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    private Integer size = 10;

    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }
}
