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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
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
        when(budgetRepository.findByUserAndCategoryAndMonthAndYear(user, foodCategory, (byte) 9, (short) 2026))
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
        assertEquals("Budget saved successfully", response.getMessage());
        assertBudgetData(response.getData(), new BigDecimal("1000000"));

        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
        verify(budgetRepository).save(budgetCaptor.capture());
        Budget savedBudget = budgetCaptor.getValue();
        assertAll(
                () -> assertEquals(user, savedBudget.getUser()),
                () -> assertEquals(foodCategory, savedBudget.getCategory()),
                () -> assertEquals(new BigDecimal("1000000"), savedBudget.getAmount()),
                () -> assertEquals(Byte.valueOf((byte) 9), savedBudget.getMonth()),
                () -> assertEquals(Short.valueOf((short) 2026), savedBudget.getYear())
        );
    }

    @Test
    void create_withExistingBudget_shouldUpdateExistingBudget() {
        CreateBudgetRequest request = createBudgetRequest(20L);

        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByUserAndCategoryAndMonthAndYear(user, foodCategory, (byte) 9, (short) 2026))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget))
                .thenReturn(budget);
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<BudgetData> response = budgetService.create(request);

        assertTrue(response.isSuccess());
        assertBudgetData(response.getData(), new BigDecimal("1000000"));

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
    void create_withCategoryOwnedByAnotherUser_shouldThrowException() {
        CreateBudgetRequest request = createBudgetRequest(21L);
        User anotherUser = new User();
        anotherUser.setId(2L);

        Category anotherUserCategory = new Category();
        anotherUserCategory.setId(21L);
        anotherUserCategory.setUser(anotherUser);
        anotherUserCategory.setCategoryIcon(foodIcon);
        anotherUserCategory.setType(CategoryType.EXPENSE);

        when(categoryRepository.findById(21L))
                .thenReturn(Optional.of(anotherUserCategory));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.create(request)
        );

        verify(budgetRepository, never()).findByUserAndCategoryAndMonthAndYear(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void create_withCategoryWithoutOwner_shouldThrowNotFoundAndNotSave() {
        CreateBudgetRequest request = createBudgetRequest(22L);

        Category categoryWithoutOwner = new Category();
        categoryWithoutOwner.setId(22L);
        categoryWithoutOwner.setCategoryIcon(foodIcon);
        categoryWithoutOwner.setType(CategoryType.EXPENSE);

        when(categoryRepository.findById(22L))
                .thenReturn(Optional.of(categoryWithoutOwner));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.create(request)
        );

        assertEquals("Category not found with id: 22", exception.getMessage());
        verify(budgetRepository, never()).findByUserAndCategoryAndMonthAndYear(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void create_withCategoryOwnerWithoutId_shouldThrowNotFoundAndNotSave() {
        CreateBudgetRequest request = createBudgetRequest(23L);
        User ownerWithoutId = new User();

        Category categoryWithOwnerWithoutId = new Category();
        categoryWithOwnerWithoutId.setId(23L);
        categoryWithOwnerWithoutId.setUser(ownerWithoutId);
        categoryWithOwnerWithoutId.setCategoryIcon(foodIcon);
        categoryWithOwnerWithoutId.setType(CategoryType.EXPENSE);

        when(categoryRepository.findById(23L))
                .thenReturn(Optional.of(categoryWithOwnerWithoutId));
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.create(request)
        );

        assertEquals("Category not found with id: 23", exception.getMessage());
        verify(budgetRepository, never()).findByUserAndCategoryAndMonthAndYear(any(), any(), any(), any());
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

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByIdAndUser(200L, user))
                .thenReturn(Optional.of(budget));
        when(budgetRepository.save(budget))
                .thenReturn(budget);
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<BudgetData> response = budgetService.updateAmount(200L, request);

        assertTrue(response.isSuccess());
        assertEquals("Update amount budget successfully", response.getMessage());
        assertBudgetData(response.getData(), new BigDecimal("1500000"));

        verify(budgetRepository).save(budget);
    }

    @Test
    void updateAmount_whenBudgetNotFound_shouldThrowException() {
        UpdateBudgetRequest request = new UpdateBudgetRequest();
        ReflectionTestUtils.setField(request, "amount", new BigDecimal("1500000"));

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByIdAndUser(99L, user))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.updateAmount(99L, request)
        );

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void delete_withExistingBudget_shouldDeleteBudget() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByIdAndUser(200L, user))
                .thenReturn(Optional.of(budget));

        budgetService.delete(200L);

        verify(budgetRepository).delete(budget);
    }

    @Test
    void delete_whenBudgetNotFound_shouldThrowException() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByIdAndUser(99L, user))
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

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByUserAndMonthAndYear(user, (byte) 9, (short) 2026))
                .thenReturn(List.of(budget));
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.of(foodIcon));

        BudgetResponse<List<BudgetData>> response = budgetService.getAll(request);

        assertTrue(response.isSuccess());
        assertEquals("Budget list fetched successfully", response.getMessage());
        assertEquals(1, response.getData().size());
        assertBudgetData(response.getData().get(0), new BigDecimal("1000000"));

        verify(budgetRepository).findByUserAndMonthAndYear(user, (byte) 9, (short) 2026);
    }

    @Test
    void getAll_whenMappedCategoryNoLongerExists_shouldThrowNotFound() {
        GetAllBudgetRequest request = new GetAllBudgetRequest();
        request.setMonth(9);
        request.setYear(2026);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByUserAndMonthAndYear(user, (byte) 9, (short) 2026))
                .thenReturn(List.of(budget));
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.getAll(request)
        );

        assertEquals("Category not found with id: 20", exception.getMessage());
        verify(categoryIconRepository, never()).findById(any());
        verify(budgetRepository, never()).save(any());
        verify(budgetRepository, never()).delete(any());
    }

    @Test
    void getAll_whenMappedCategoryIconNoLongerExists_shouldThrowNotFound() {
        GetAllBudgetRequest request = new GetAllBudgetRequest();
        request.setMonth(9);
        request.setYear(2026);

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(budgetRepository.findByUserAndMonthAndYear(user, (byte) 9, (short) 2026))
                .thenReturn(List.of(budget));
        when(categoryRepository.findById(20L))
                .thenReturn(Optional.of(foodCategory));
        when(categoryIconRepository.findById(2L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> budgetService.getAll(request)
        );

        assertEquals("Category icon not found with id: 2", exception.getMessage());
        verify(budgetRepository, never()).save(any());
        verify(budgetRepository, never()).delete(any());
    }

    private CreateBudgetRequest createBudgetRequest(Long categoryId) {
        CreateBudgetRequest request = new CreateBudgetRequest();
        request.setCategoryId(categoryId);
        request.setAmount(new BigDecimal("1000000"));
        request.setMonth(9);
        request.setYear(2026);
        return request;
    }

    private void assertBudgetData(BudgetData data, BigDecimal expectedAmount) {
        assertNotNull(data);
        assertEquals(200L, data.getId());
        assertEquals(0, data.getAmount().compareTo(expectedAmount));
        assertEquals(9, data.getMonth());
        assertEquals(2026, data.getYear());
        assertEquals(CategoryType.EXPENSE, data.getType());
        assertNotNull(data.getCategory());
        assertEquals(20L, data.getCategory().getId());
        assertEquals("Food", data.getCategory().getName());
        assertEquals("food-icon", data.getCategory().getIcon());
        assertEquals("food.png", data.getCategory().getIconUrl());
    }
}
