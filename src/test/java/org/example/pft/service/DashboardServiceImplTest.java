package org.example.pft.service;

import org.example.pft.dto.dashboard.DashboardResponse;
import org.example.pft.dto.dashboard.PieChartData;
import org.example.pft.dto.dashboard.RecentTransData;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @InjectMocks
    DashboardServiceImpl dashboardService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
    }

    @Test
    void showDashboard_shouldReturnDashboardData() {
        List<PieChartData> pieChart = List.of(
                new PieChartData("Salary", new BigDecimal("10000000"), CategoryType.INCOME),
                new PieChartData("Food", new BigDecimal("3000000"), CategoryType.EXPENSE)
        );
        List<RecentTransData> recentTransactions = List.of(
                new RecentTransData(
                        100L,
                        "Food",
                        "food-icon",
                        new BigDecimal("-3000000"),
                        LocalDate.of(2026, 9, 7),
                        CategoryType.EXPENSE
                )
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.INCOME))
                .thenReturn(new BigDecimal("10000000"));
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.EXPENSE))
                .thenReturn(new BigDecimal("3000000"));
        when(transactionRepository.findPieChartData(1L, 9, 2026))
                .thenReturn(pieChart);
        when(transactionRepository.findRecentTransData(eq(1L), eq(9), eq(2026), any(Pageable.class)))
                .thenReturn(recentTransactions);

        DashboardResponse response = dashboardService.showDashboard(9, 2026);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(100L, response.getData().getRecentTransactions().get(0).getId());

        verify(transactionRepository).findRecentTransData(
                eq(1L),
                eq(9),
                eq(2026),
                any(Pageable.class)
        );
    }

    @Test
    void showDashboard_whenTotalsAreNull_shouldUseZeroTotals() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.INCOME))
                .thenReturn(null);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.EXPENSE))
                .thenReturn(null);
        when(transactionRepository.findPieChartData(1L, 9, 2026))
                .thenReturn(List.of());
        when(transactionRepository.findRecentTransData(eq(1L), eq(9), eq(2026), any(Pageable.class)))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.showDashboard(9, 2026);

        assertTrue(response.isSuccess());
        assertTrue(response.getData().getPieChart().isEmpty());
        assertTrue(response.getData().getRecentTransactions().isEmpty());
    }

    @Test
    void showDashboard_withoutAuthentication_shouldThrowUnauthenticated() {
        when(currentUserHelper.getCurrentUser())
                .thenThrow(new RuntimeException("Unauthenticated"));

        assertThrows(
                RuntimeException.class,
                () -> dashboardService.showDashboard(9, 2026)
        );

        verify(transactionRepository, never()).getTotalByType(any(), any(), any(), any());
        verify(transactionRepository, never()).findPieChartData(any(), any(), any());
        verify(transactionRepository, never()).findRecentTransData(any(), any(), any(), any());
    }
}
