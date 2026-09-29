package org.example.pft.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({"success","message","data"})
public class DashboardResponse {
    private boolean success;
    private String message;
    private DashboardData data;
}
