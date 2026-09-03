package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.*;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.monthly.MonthlyRequest;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/reports")
@AllArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/category")
    public ResponseEntity<ReportResponse<ReportCategoryData>> showReportCategory(
            @Valid @ModelAttribute ReportRequest request){
        return ResponseEntity.ok().body(reportService
                .showReportCategory(request.getMonth(),request.getYear(),request.getType()));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ReportResponse<MonthlyData>> showMonthly(
            @Valid @ModelAttribute MonthlyRequest request
            ){
        return ResponseEntity.ok().body(reportService.showMonthly(request.getMonth(),request.getYear()));
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportResponse<SummaryData>> showSummary(
            @Valid @ModelAttribute MonthlyRequest request
    ){
        return ResponseEntity.ok().body(reportService.showSummary(request.getMonth(),request.getYear()));
    }
}
