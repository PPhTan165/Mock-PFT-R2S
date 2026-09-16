package org.example.pft.dto.email;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmailExportRequest {

    @NotNull(message = "Month is required")
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer month;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be between 1900 and 9999")
    @Max(value = 9999, message = "Year must be between 1900 and 9999")
    private Integer year;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;

    private Boolean includeChart;

    private Boolean includeTopExpenses;

}
