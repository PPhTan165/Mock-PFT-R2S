package org.example.pft.dto.report;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.time.LocalDate;

@Data
public class ReportRequest {
    @Min(value = 1, message = "Month must be in range from 1-12")
    @Max(value = 12, message = "Month must be in range from 1-12")
    private Integer month = LocalDate.now().getMonthValue();

    private Integer year = LocalDate.now().getYear();

    @NotNull(message = "Type is required")
    private CategoryType type;

    public void setMonth(Integer month) {
        this.month = month == null ? LocalDate.now().getMonthValue() : month;
    }

    public void setYear(Integer year) {
        this.year = year == null ? LocalDate.now().getYear() : year;
    }
}
