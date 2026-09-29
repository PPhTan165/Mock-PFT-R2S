package org.example.pft.dto.transaction;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.example.pft.enums.CategoryType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class HistoryRequest {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 20;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private Long categoryId;

    @NotNull(message = "Type is required INCOME or EXPENSE")
    private CategoryType type;

    @NotNull(message = "Page is required")
    @Min(value = 1, message = "Page must be greater than or equal to 1")
    private Integer page = DEFAULT_PAGE;

    @NotNull(message = "Size is required")
    @Min(value = 1, message = "Size must be greater than or equal to 1")
    @Max(value = MAX_SIZE, message = "Size must be less than or equal to 20")
    private Integer size = DEFAULT_SIZE;

    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean isValidDateRange() {
        if (startDate == null || endDate == null) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }
}
