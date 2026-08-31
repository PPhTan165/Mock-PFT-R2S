package org.example.pft.dto.report;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"success","message","data"})
public class ReportResponse {
    private boolean success;
    private String message;
    private ReportCategoryData data;
}
