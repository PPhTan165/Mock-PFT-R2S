package org.example.pft.dto.report.monthly;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MonthlyRequest {
    @Min(value = 1, message = "Month must be in range from 1-12")
    @Max(value = 12, message = "Month must be in range from 1-12")
    private Integer month = LocalDate.now().getMonthValue();

    private Integer year = LocalDate.now().getYear();

    public void setMonth(Integer month) {
        this.month = month == null ? LocalDate.now().getMonthValue() : month;
    }

    public void setYear(Integer year) {
        this.year = year == null ? LocalDate.now().getYear() : year;
    }
}
