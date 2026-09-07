package org.example.pft.service;

import org.example.pft.dto.report.ReportResponse;
import org.example.pft.dto.report.category.ReportCategory;
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

        ReportResponse<?> response = reportService.showReportCategory(MONTH, YEAR, CategoryType.EXPENSE);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(currentUserHelper).getCurrentUser();
        verify(categoryRepository).findReportCategoryData(CategoryType.EXPENSE, USER_ID, MONTH, YEAR);
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
        when(transactionRepository.showTopExpenses(USER_ID, MONTH, YEAR, CategoryType.EXPENSE))
                .thenReturn(topExpenses);

        ReportResponse<?> response = reportService.showSummary(MONTH, YEAR);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        verify(currentUserHelper).getCurrentUser();
        verify(transactionRepository).showTopExpenses(USER_ID, MONTH, YEAR, CategoryType.EXPENSE);
    }
}
