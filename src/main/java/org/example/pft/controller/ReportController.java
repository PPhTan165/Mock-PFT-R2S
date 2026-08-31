package org.example.pft.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.pft.dto.report.ReportRequest;
import org.example.pft.dto.report.ReportResponse;
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
    public ResponseEntity<ReportResponse> showReportCategory(@Valid @ModelAttribute ReportRequest request){
        return ResponseEntity.ok().body(reportService.showReportCategory(request.getMonth(),request.getYear(),request.getType()));
    }
}
