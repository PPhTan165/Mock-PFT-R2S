package org.example.pft.service;

import org.example.pft.dto.dashboard.DashboardResponse;

public interface DashboardService {
    DashboardResponse showDashboard(Integer month, Integer year);
}
