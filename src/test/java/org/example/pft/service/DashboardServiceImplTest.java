package org.example.pft.service;

import org.example.pft.dto.dashboard.DashboardData;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
                new PieChartData("Salary", new BigDecimal("10000000.75"), CategoryType.INCOME),
                new PieChartData("Food", new BigDecimal("3000000.25"), CategoryType.EXPENSE),
                new PieChartData("Bonus", new BigDecimal("2000000.50"), CategoryType.INCOME)
        );
        List<RecentTransData> recentTransactions = List.of(
                new RecentTransData(
                        100L,
                        "Food",
                        "burger",
                        new BigDecimal("-3000000.25"),
                        LocalDate.of(2026, 9, 7),
                        CategoryType.EXPENSE
                ),
                new RecentTransData(
                        101L,
                        "Salary",
                        "money",
                        new BigDecimal("10000000.75"),
                        LocalDate.of(2026, 9, 6),
                        CategoryType.INCOME
                )
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.INCOME))
                .thenReturn(new BigDecimal("12000001.25"));
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.EXPENSE))
                .thenReturn(new BigDecimal("3000000.25"));
        when(transactionRepository.findPieChartData(1L, 9, 2026))
                .thenReturn(pieChart);
        when(transactionRepository.findRecentTransData(eq(1L), eq(9), eq(2026), any(Pageable.class)))
                .thenReturn(recentTransactions);

        DashboardResponse response = dashboardService.showDashboard(9, 2026);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Dashboard data fetched successfully", response.getMessage());

        DashboardData data = response.getData();
        assertNotNull(data);
        assertAmount("12000001.25", data.getIncome());
        assertAmount("3000000.25", data.getExpense());
        assertAmount("9000001.00", data.getBalance());

        assertEquals(3, data.getPieChart().size());
        assertPieChart(data.getPieChart().get(0), "Salary", "10000000.75", CategoryType.INCOME);
        assertPieChart(data.getPieChart().get(1), "Food", "3000000.25", CategoryType.EXPENSE);
        assertPieChart(data.getPieChart().get(2), "Bonus", "2000000.50", CategoryType.INCOME);

        assertEquals(2, data.getRecentTransactions().size());
        assertRecentTransaction(
                data.getRecentTransactions().get(0),
                100L,
                "Food",
                "burger",
                "-3000000.25",
                LocalDate.of(2026, 9, 7),
                CategoryType.EXPENSE
        );
        assertRecentTransaction(
                data.getRecentTransactions().get(1),
                101L,
                "Salary",
                "money",
                "10000000.75",
                LocalDate.of(2026, 9, 6),
                CategoryType.INCOME
        );

        assertRecentTransactionsPageRequest(9, 2026);
        verify(transactionRepository).getTotalByType(1L, 9, 2026, CategoryType.INCOME);
        verify(transactionRepository).getTotalByType(1L, 9, 2026, CategoryType.EXPENSE);
        verify(transactionRepository).findPieChartData(1L, 9, 2026);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void showDashboard_withoutTransactions_shouldUseZeroTotalsAndEmptyLists() {
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
        DashboardData data = response.getData();
        assertNotNull(data);
        assertAmount("0", data.getIncome());
        assertAmount("0", data.getExpense());
        assertAmount("0", data.getBalance());
        assertTrue(data.getPieChart().isEmpty());
        assertTrue(data.getRecentTransactions().isEmpty());
        assertRecentTransactionsPageRequest(9, 2026);
        verify(transactionRepository).getTotalByType(1L, 9, 2026, CategoryType.INCOME);
        verify(transactionRepository).getTotalByType(1L, 9, 2026, CategoryType.EXPENSE);
        verify(transactionRepository).findPieChartData(1L, 9, 2026);
        verifyNoMoreInteractions(transactionRepository);
    }

    @Test
    void showDashboard_withZeroIncomeAndNonzeroExpenses_shouldReturnNegativeBalance() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.EXPENSE))
                .thenReturn(new BigDecimal("350.40"));
        when(transactionRepository.findPieChartData(1L, 9, 2026))
                .thenReturn(List.of(new PieChartData("Food", new BigDecimal("350.40"), CategoryType.EXPENSE)));
        when(transactionRepository.findRecentTransData(eq(1L), eq(9), eq(2026), any(Pageable.class)))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.showDashboard(9, 2026);

        DashboardData data = response.getData();
        assertNotNull(data);
        assertAmount("0", data.getIncome());
        assertAmount("350.40", data.getExpense());
        assertAmount("-350.40", data.getBalance());
        assertEquals(1, data.getPieChart().size());
        assertPieChart(data.getPieChart().get(0), "Food", "350.40", CategoryType.EXPENSE);
        assertTrue(data.getRecentTransactions().isEmpty());
        assertRecentTransactionsPageRequest(9, 2026);
    }

    @Test
    void showDashboard_withIncomeAndZeroExpenses_shouldReturnPositiveBalance() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.INCOME))
                .thenReturn(new BigDecimal("700.30"));
        when(transactionRepository.getTotalByType(1L, 9, 2026, CategoryType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.findPieChartData(1L, 9, 2026))
                .thenReturn(List.of(new PieChartData("Salary", new BigDecimal("700.30"), CategoryType.INCOME)));
        when(transactionRepository.findRecentTransData(eq(1L), eq(9), eq(2026), any(Pageable.class)))
                .thenReturn(List.of());

        DashboardResponse response = dashboardService.showDashboard(9, 2026);

        DashboardData data = response.getData();
        assertNotNull(data);
        assertAmount("700.30", data.getIncome());
        assertAmount("0", data.getExpense());
        assertAmount("700.30", data.getBalance());
        assertEquals(1, data.getPieChart().size());
        assertPieChart(data.getPieChart().get(0), "Salary", "700.30", CategoryType.INCOME);
        assertTrue(data.getRecentTransactions().isEmpty());
        assertRecentTransactionsPageRequest(9, 2026);
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

    private void assertRecentTransactionsPageRequest(Integer month, Integer year) {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(transactionRepository).findRecentTransData(
                eq(1L),
                eq(month),
                eq(year),
                pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(3, pageable.getPageSize());
        assertTrue(pageable.getSort().isUnsorted());
    }

    private void assertPieChart(
            PieChartData pieChartData,
            String expectedCategory,
            String expectedAmount,
            CategoryType expectedType
    ) {
        assertEquals(expectedCategory, pieChartData.getCategory());
        assertAmount(expectedAmount, pieChartData.getAmount());
        assertEquals(expectedType, pieChartData.getType());
    }

    private void assertRecentTransaction(
            RecentTransData recentTransData,
            Long expectedId,
            String expectedCategory,
            String expectedIcon,
            String expectedAmount,
            LocalDate expectedDate,
            CategoryType expectedType
    ) {
        assertEquals(expectedId, recentTransData.getId());
        assertEquals(expectedCategory, recentTransData.getCategory());
        assertEquals(expectedIcon, recentTransData.getIcon());
        assertAmount(expectedAmount, recentTransData.getAmount());
        assertEquals(expectedDate, recentTransData.getDate());
        assertEquals(expectedType, recentTransData.getType());
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
