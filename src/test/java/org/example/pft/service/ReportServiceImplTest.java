package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.category.ReportCategoryData;
import org.example.pft.dto.report.summary.SummaryData;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.repository.TransactionRepository;
import org.example.pft.service.impl.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Integer MONTH = 9;
    private static final Integer YEAR = 2026;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @InjectMocks
    ReportServiceImpl reportService;

    private User user;
    private List<ReportCategory> categories;
    private List<TopExpenses> topExpenses;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(USER_ID);

        categories = List.of(
                new ReportCategory("Food", new BigDecimal("300.00")),
                new ReportCategory("Transport", new BigDecimal("100.00"))
        );

        topExpenses = List.of(
                new TopExpenses("Food", "burger", null, new BigDecimal("200.00")),
                new TopExpenses("Transport", "bus", null, new BigDecimal("100.00"))
        );
    }

    @Test
    void showReportCategory_shouldReturnSuccessResponse() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR))
                .thenReturn(categories);

        ReportResponse<ReportCategoryData> response =
                reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category breakdown fetched successfully", response.getMessage());

        ReportCategoryData data = response.getData();
        assertNotNull(data);
        assertEquals(CategoryType.EXPENSE, data.getType());
        assertAmount("400.00", data.getTotal());
        assertEquals(2, data.getCategories().size());
        assertCategory(data.getCategories().get(0), "Food", "300.00", "75.00");
        assertCategory(data.getCategories().get(1), "Transport", "100.00", "25.00");

        verify(currentUserHelper).getCurrentUser();
        verify(categoryRepository).findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR);
    }

    @Test
    void showReportCategory_forIncome_shouldSetTotalAndPercentages() {
        List<ReportCategory> incomeCategories = List.of(
                new ReportCategory("Salary", new BigDecimal("7000.00")),
                new ReportCategory("Bonus", new BigDecimal("3000.00"))
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findReportCategoryData(CategoryType.INCOME, USER_ID, MONTH, YEAR))
                .thenReturn(incomeCategories);

        ReportResponse<ReportCategoryData> response =
                reportService.showReportCategory(MONTH, YEAR, CategoryType.INCOME);

        ReportCategoryData data = response.getData();
        assertNotNull(data);
        assertEquals(CategoryType.INCOME, data.getType());
        assertAmount("10000.00", data.getTotal());
        assertCategory(data.getCategories().get(0), "Salary", "7000.00", "70.00");
        assertCategory(data.getCategories().get(1), "Bonus", "3000.00", "30.00");

        verify(categoryRepository).findReportCategoryData(CategoryType.INCOME, USER_ID, MONTH, YEAR);
    }

    @Test
    void showReportCategory_whenRepositoryReturnsEmptyList_shouldReturnZeroTotalAndEmptyCategories() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR))
                .thenReturn(List.of());

        ReportResponse<ReportCategoryData> response =
                reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);

        ReportCategoryData data = response.getData();
        assertNotNull(data);
        assertEquals(CategoryType.EXPENSE, data.getType());
        assertAmount("0", data.getTotal());
        assertTrue(data.getCategories().isEmpty());

        verify(categoryRepository).findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR);
    }

    @Test
    void showReportCategory_whenTotalIsZero_shouldSetZeroPercentages() {
        List<ReportCategory> zeroCategories = List.of(
                new ReportCategory("Food", BigDecimal.ZERO),
                new ReportCategory("Transport", new BigDecimal("0.00"))
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR))
                .thenReturn(zeroCategories);

        ReportResponse<ReportCategoryData> response =
                reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);

        ReportCategoryData data = response.getData();
        assertNotNull(data);
        assertAmount("0", data.getTotal());
        assertCategory(data.getCategories().get(0), "Food", "0", "0");
        assertCategory(data.getCategories().get(1), "Transport", "0.00", "0");
    }

    @Test
    void showReportCategory_shouldRoundPercentagesToTwoDecimalsHalfUp() {
        List<ReportCategory> roundingCategories = List.of(
                new ReportCategory("Food", new BigDecimal("1.00")),
                new ReportCategory("Transport", new BigDecimal("2.00"))
        );

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR))
                .thenReturn(roundingCategories);

        ReportResponse<ReportCategoryData> response =
                reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);

        ReportCategoryData data = response.getData();
        assertNotNull(data);
        assertAmount("3.00", data.getTotal());
        assertCategory(data.getCategories().get(0), "Food", "1.00", "33.33");
        assertCategory(data.getCategories().get(1), "Transport", "2.00", "66.67");
    }

    @Test
    void showMonthly_shouldReturnSuccessResponse() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        when(transactionRepository.getTotalByType(
                eq(USER_ID),
                anyInt(),
                eq(YEAR),
                eq(CategoryType.INCOME)))
                .thenReturn(new BigDecimal("1000.00"));

        when(transactionRepository.getTotalByType(
                eq(USER_ID),
                anyInt(),
                eq(YEAR),
                eq(CategoryType.EXPENSE)))
                .thenReturn(new BigDecimal("300.00"));

        ReportResponse<?> response = reportService.showMonthly(MONTH, YEAR);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(currentUserHelper).getCurrentUser();
        verify(transactionRepository, times(13))
                .getTotalByType(eq(USER_ID), anyInt(), eq(YEAR), eq(CategoryType.INCOME));
        verify(transactionRepository, times(13))
                .getTotalByType(eq(USER_ID), anyInt(), eq(YEAR), eq(CategoryType.EXPENSE));
    }

    @Test
    void showSummary_shouldReturnSuccessResponse() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(new BigDecimal("350.00"));
        when(transactionRepository.showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(topExpenses);

        ReportResponse<SummaryData> response = reportService.showSummary(MONTH, YEAR);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Monthly summary fetched successfully", response.getMessage());

        SummaryData data = response.getData();
        assertNotNull(data);
        assertEquals("September", data.getMonth());
        assertEquals((short) YEAR.intValue(), data.getYear());
        assertAmount("1000.00", data.getIncome());
        assertAmount("350.00", data.getExpense());
        assertAmount("650.00", data.getBalance());
        assertEquals(2, data.getTopExpenses().size());
        assertTopExpense(data.getTopExpenses().get(0), "Food", "200.00", "57.14");
        assertTopExpense(data.getTopExpenses().get(1), "Transport", "100.00", "28.57");

        verify(currentUserHelper).getCurrentUser();
        verify(transactionRepository).getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME);
        verify(transactionRepository).getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE);
        verify(transactionRepository).showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE);
    }

    @Test
    void showSummary_whenTotalsAreNullAndNoTopExpenses_shouldReturnZeroSummary() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME))
                .thenReturn(null);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(null);
        when(transactionRepository.showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(List.of());

        ReportResponse<SummaryData> response = reportService.showSummary(MONTH, YEAR);

        SummaryData data = response.getData();
        assertNotNull(data);
        assertEquals("September", data.getMonth());
        assertEquals((short) YEAR.intValue(), data.getYear());
        assertAmount("0", data.getIncome());
        assertAmount("0", data.getExpense());
        assertAmount("0", data.getBalance());
        assertTrue(data.getTopExpenses().isEmpty());

        verify(transactionRepository).getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME);
        verify(transactionRepository).getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE);
        verify(transactionRepository).showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE);
    }

    @Test
    void showSummary_withZeroIncomeAndNonzeroExpenses_shouldReturnNegativeBalance() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(new BigDecimal("300.00"));
        when(transactionRepository.showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(List.of());

        ReportResponse<SummaryData> response = reportService.showSummary(MONTH, YEAR);

        SummaryData data = response.getData();
        assertNotNull(data);
        assertAmount("0", data.getIncome());
        assertAmount("300.00", data.getExpense());
        assertAmount("-300.00", data.getBalance());
    }

    @Test
    void showSummary_withIncomeAndZeroExpenses_shouldReturnPositiveBalance() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.INCOME))
                .thenReturn(new BigDecimal("500.00"));
        when(transactionRepository.getTotalByType(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.showTopCategories(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(List.of());

        ReportResponse<SummaryData> response = reportService.showSummary(MONTH, YEAR);

        SummaryData data = response.getData();
        assertNotNull(data);
        assertAmount("500.00", data.getIncome());
        assertAmount("0", data.getExpense());
        assertAmount("500.00", data.getBalance());
    }

    private void assertCategory(
            ReportCategory category,
            String expectedCategory,
            String expectedAmount,
            String expectedPercentage
    ) {
        assertEquals(expectedCategory, category.getCategory());
        assertAmount(expectedAmount, category.getAmount());
        assertDecimal(expectedPercentage, category.getPercentage());
    }

    private void assertTopExpense(
            TopExpenses topExpense,
            String expectedCategory,
            String expectedAmount,
            String expectedPercentage
    ) {
        assertEquals(expectedCategory, topExpense.getCategory());
        assertAmount(expectedAmount, topExpense.getAmount());
        assertDecimal(expectedPercentage, topExpense.getPercentage());
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(new BigDecimal(expected), actual);
    }
}
