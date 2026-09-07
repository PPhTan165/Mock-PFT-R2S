package org.example.pft.controller;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.monthly.ChartData;
import org.example.pft.dto.report.monthly.MonthlyData;
import org.example.pft.dto.report.monthly.SummaryMonthlyData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.enums.CategoryType;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class ReportControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ReportService reportService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private ReportResponse<ReportCategoryData> categoryResponse;
    private ReportResponse<MonthlyData> monthlyResponse;
    private ReportResponse<SummaryData> summaryResponse;

    @BeforeEach
    void setup() {
        categoryResponse = new ReportResponse<>();
        categoryResponse.setSuccess(true);
        categoryResponse.setMessage("Category breakdown fetched successfully");
        categoryResponse.setData(new ReportCategoryData(
                CategoryType.EXPENSE,
                new BigDecimal("3000000"),
                List.of(new ReportCategory("Food", new BigDecimal("3000000")))
        ));

        monthlyResponse = new ReportResponse<>();
        monthlyResponse.setSuccess(true);
        monthlyResponse.setMessage("Monthly financial report fetched successfully");
        monthlyResponse.setData(new MonthlyData(
                List.of(new ChartData("SEP", new BigDecimal("10000000"), new BigDecimal("3000000"))),
                new SummaryMonthlyData("September 2026", new BigDecimal("10000000"), new BigDecimal("3000000"))
        ));

        summaryResponse = new ReportResponse<>();
        summaryResponse.setSuccess(true);
        summaryResponse.setMessage("Monthly summary fetched successfully");
        summaryResponse.setData(new SummaryData(
                "September",
                (short) 2026,
                new BigDecimal("10000000"),
                new BigDecimal("3000000"),
                new BigDecimal("7000000"),
                List.of(new TopExpenses("Food", "food-icon", "food.png", new BigDecimal("3000000")))
        ));
    }

    @Test
    void showReportCategory_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/reports/category")
                        .param("month", "9")
                        .param("year", "2026")
                        .param("type", "EXPENSE"))
                .andExpect(status().isUnauthorized());

        verify(reportService, never()).showReportCategory(9, 2026, CategoryType.EXPENSE);
    }

    @Test
    @WithMockUser
    void showReportCategory_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(reportService.showReportCategory(9, 2026, CategoryType.EXPENSE))
                .thenReturn(categoryResponse);

        mockMvc.perform(get("/api/reports/category")
                        .param("month", "9")
                        .param("year", "2026")
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk());

        verify(reportService).showReportCategory(9, 2026, CategoryType.EXPENSE);
    }

    @Test
    @WithMockUser
    void showReportCategory_withoutType_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/reports/category")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isUnprocessableContent());

        verify(reportService, never()).showReportCategory(anyInt(), anyInt(), any());
    }

    @Test
    @WithMockUser
    void showMonthly_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(reportService.showMonthly(9, 2026))
                .thenReturn(monthlyResponse);

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk());

        verify(reportService).showMonthly(9, 2026);
    }

    @Test
    @WithMockUser
    void showMonthly_withInvalidMonth_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isUnprocessableContent());

        verify(reportService, never()).showMonthly(13, 2026);
    }

    @Test
    @WithMockUser
    void showSummary_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(reportService.showSummary(9, 2026))
                .thenReturn(summaryResponse);

        mockMvc.perform(get("/api/reports/summary")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk());

        verify(reportService).showSummary(9, 2026);
    }

    @Test
    @WithMockUser
    void showSummary_withInvalidMonth_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/reports/summary")
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isUnprocessableContent());

        verify(reportService, never()).showSummary(13, 2026);
    }
}
