package org.example.pft.service;

import org.example.pft.dto.budget.BudgetData;
import org.example.pft.dto.budget.BudgetResponse;
import org.example.pft.dto.budget.CreateBudgetRequest;
import org.example.pft.dto.budget.GetAllBudgetRequest;
import org.example.pft.dto.budget.UpdateBudgetRequest;
import org.example.pft.entity.Budget;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.BudgetRepository;
import org.example.pft.repository.CategoryIconRepository;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.service.impl.BudgetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CategoryIconRepository categoryIconRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @Mock
    BudgetRepository budgetRepository;

    @InjectMocks
    BudgetServiceImpl budgetService;

    private User user;
    private CategoryIcon foodIcon;
    private Category foodCategory;
    private Category salaryCategory;
    private Budget budget;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);

        foodIcon = new CategoryIcon();
        foodIcon.setId(2L);
        foodIcon.setCategoryName("Food");
        foodIcon.setEmoji("food-icon");
        foodIcon.setIconUrl("food.png");

        foodCategory = new Category();
        foodCategory.setId(20L);
        foodCategory.setUser(user);
        foodCategory.setCategoryIcon(foodIcon);
        foodCategory.setType(CategoryType.EXPENSE);

        CategoryIcon salaryIcon = new CategoryIcon();
        salaryIcon.setId(3L);

        salaryCategory = new Category();
        salaryCategory.setId(30L);
        salaryCategory.setCategoryIcon(salaryIcon);
        salaryCategory.setType(CategoryType.INCOME);

        budget = new Budget();
        budget.setId(200L);
        budget.setUser(user);
        budget.setCategory(foodCategory);
        budget.setAmount(new BigDecimal("1000000"));
        budget.setMonth((byte) 9);
        budget.setYear((short) 2026);
    }

    @Test
    void create_withValidExpenseCategory_shouldSaveBudgetAndReturnResponse() {
        CreateBudgetRequest request = createBudgetRequest(20L);

        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByCategoryAndMonthAndYear(foodCategory, (byte) 9, (short) 2026))
                .thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> {
                    Budget savedBudget = invocation.getArgument(0);
                    savedBudget.setId(200L);
                    return savedBudget;
                });
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<BudgetData> response = budgetService.create(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(200L, response.getData().getId());
        assertEquals(20L, response.getData().getCategory().getId());

        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void create_withExistingBudget_shouldUpdateExistingBudget() {
        CreateBudgetRequest request = createBudgetRequest(20L);

        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByCategoryAndMonthAndYear(foodCategory, (byte) 9, (short) 2026))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget))
                .thenReturn(budget);
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<BudgetData> response = budgetService.create(request);

        assertTrue(response.isSuccess());
        assertEquals(200L, response.getData().getId());
        assertEquals(20L, response.getData().getCategory().getId());

        verify(budgetRepository).save(budget);
    }

    @Test
    void create_withIncomeCategory_shouldThrowException() {
        CreateBudgetRequest request = createBudgetRequest(30L);

        when(categoryRepository.findById(30L))
                .thenReturn(Optional.of(salaryCategory));

        assertThrows(
                BusinessValidationException.class,
                () -> budgetService.create(request)
        );

        verify(currentUserHelper, never()).getCurrentUser();
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void create_whenCategoryNotFound_shouldThrowException() {
        CreateBudgetRequest request = createBudgetRequest(99L);

        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.create(request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateAmount_withExistingBudget_shouldReturnUpdatedBudget() {
        UpdateBudgetRequest request = new UpdateBudgetRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal("1500000"));

        when(budgetRepository.findById(200L))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget))
                .thenReturn(budget);
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<BudgetData> response = budgetService.updateAmount(200L, request);

        assertTrue(response.isSuccess());
        assertEquals(200L, response.getData().getId());
        assertEquals(20L, response.getData().getCategory().getId());

        verify(budgetRepository).save(budget);
    }

    @Test
    void updateAmount_whenBudgetNotFound_shouldThrowException() {
        UpdateBudgetRequest request = new UpdateBudgetRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal("1500000"));

        when(budgetRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.updateAmount(99L, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void delete_withExistingBudget_shouldDeleteBudget() {
        when(budgetRepository.findById(200L))
                .thenReturn(Optional.of(budget));

        budgetService.delete(200L);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void delete_whenBudgetNotFound_shouldThrowException() {
        when(budgetRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.delete(99L)
        );

        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void getAll_shouldReturnBudgetsByMonthAndYear() {
        GetAllBudgetRequest request = new GetAllBudgetRequest();
        request.setMonth(9);
        request.setYear(2026);

        when(budgetRepository.findByMonthAndYear((byte) 9, (short) 2026))
                .thenReturn(List.of(budget));
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<List<BudgetData>> response = budgetService.getAll(request);

        assertTrue(response.isSuccess());
        assertEquals(200L, response.getData().get(0).getId());
        assertEquals(20L, response.getData().get(0).getCategory().getId());

        verify(budgetRepository).findByMonthAndYear((byte) 9, (short) 2026);
    }

    private CreateBudgetRequest createBudgetRequest(Long categoryId) {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryId);
        request.setAmount(new BigDecimal("1000000"));
        request.setMonth(9);
        request.setYear(2026);
        return request;
    }
}
