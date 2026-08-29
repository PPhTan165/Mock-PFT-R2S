package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.dashboard.DashboardRequest;
import org.example.pft.dto.dashboard.DashboardResponse;
import org.example.pft.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@AllArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> showDashboard(@Valid @ModelAttribute DashboardRequest request){
        return ResponseEntity.ok().body(dashboardService.showDashboard(request.getMonth(), request.getYear()));
    }
}
