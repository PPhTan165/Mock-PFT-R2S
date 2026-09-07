package org.example.pft.controller;

import org.example.pft.dto.dashboard.DashboardData;
import org.example.pft.dto.dashboard.DashboardResponse;
import org.example.pft.dto.dashboard.PieChartData;
import org.example.pft.dto.dashboard.RecentTransData;
import org.example.pft.enums.CategoryType;
import org.example.pft.security.CustomUserDetailsService;
import org.example.pft.security.JwtAuthenticationFilter;
import org.example.pft.security.JwtService;
import org.example.pft.security.RestAccessDeniedHandler;
import org.example.pft.security.RestAuthenticationEntityPoint;
import org.example.pft.security.SecurityConfig;
import org.example.pft.service.DashboardService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntityPoint.class,
        RestAccessDeniedHandler.class
})
class DashboardControllerSecurityTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DashboardService dashboardService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService userDetailsService;

    private DashboardResponse dashboardResponse;

    @BeforeEach
    void setup() {
        dashboardResponse = new DashboardResponse();
        dashboardResponse.setSuccess(true);
        dashboardResponse.setMessage("Dashboard data fetched successfully");
        dashboardResponse.setData(new DashboardData(
                new BigDecimal("10000000"),
                new BigDecimal("3000000"),
                new BigDecimal("7000000"),
                List.of(new PieChartData("Food", new BigDecimal("3000000"), CategoryType.EXPENSE)),
                List.of(new RecentTransData(
                        100L,
                        "Food",
                        "food-icon",
                        new BigDecimal("-3000000"),
                        LocalDate.of(2026, 9, 7),
                        CategoryType.EXPENSE
                ))
        ));
    }

    @Test
    void showDashboard_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isUnauthorized());

        verify(dashboardService, never()).showDashboard(9, 2026);
    }

    @Test
    @WithMockUser
    void showDashboard_withAuthenticatedUser_shouldReturn200() throws Exception {
        when(dashboardService.showDashboard(9, 2026))
                .thenReturn(dashboardResponse);

        mockMvc.perform(get("/api/dashboard")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recentTransactions[0].id").value(100));

        verify(dashboardService).showDashboard(9, 2026);
    }

    @Test
    @WithMockUser
    void showDashboard_withInvalidMonth_shouldReturn422() throws Exception {
        mockMvc.perform(get("/api/dashboard")
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isUnprocessableContent());

        verify(dashboardService, never()).showDashboard(13, 2026);
    }
}
