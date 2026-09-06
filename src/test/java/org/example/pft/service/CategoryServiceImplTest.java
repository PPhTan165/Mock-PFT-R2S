package org.example.pft.service;

import org.example.pft.dto.category.CategoryByTypeData;
import org.example.pft.dto.category.CategoryData;
import org.example.pft.dto.category.CategoryRequest;
import org.example.pft.dto.category.CategoryResponse;
import org.example.pft.dto.category.CreateCategoryData;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.example.pft.exception.BusinessConflictException;
import org.example.pft.exception.BusinessValidationException;
import org.example.pft.exception.ResourceNotFoundException;
import org.example.pft.helper.CurrentUserHelper;
import org.example.pft.repository.CategoryIconRepository;
import org.example.pft.repository.CategoryRepository;
import org.example.pft.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CurrentUserHelper currentUserHelper;

    @Mock
    CategoryIconRepository categoryIconRepository;

    @InjectMocks
    CategoryServiceImpl categoryService;

    private User user;
    private CategoryIcon salaryIcon;
    private CategoryIcon foodIcon;
    private Category salaryCategory;
    private Category foodCategory;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");

        salaryIcon = createIcon(1L, "Salary", "money-icon", "salary.png");
        foodIcon = createIcon(2L, "Food", "food-icon", "food.png");

        salaryCategory = createCategory(10L, user, salaryIcon, CategoryType.INCOME);
        foodCategory = createCategory(20L, user, foodIcon, CategoryType.EXPENSE);
    }

    @Test
    void getAll_shouldReturnCategoriesGroupedByType() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findAllByUser(user))
                .thenReturn(List.of(salaryCategory, foodCategory));

        CategoryResponse<Map<CategoryType, List<CategoryData>>> response = categoryService.getAll();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category list fetched successfully", response.getMessage());

        Map<CategoryType, List<CategoryData>> data = response.getData();
        assertEquals(1, data.get(CategoryType.INCOME).size());
        assertEquals(1, data.get(CategoryType.EXPENSE).size());

        CategoryData income = data.get(CategoryType.INCOME).get(0);
        assertEquals(10L, income.getId());
        assertEquals("Salary", income.getName());
        assertEquals("money-icon", income.getIcon());

        CategoryData expense = data.get(CategoryType.EXPENSE).get(0);
        assertEquals(20L, expense.getId());
        assertEquals("Food", expense.getName());
        assertEquals("food-icon", expense.getIcon());

        verify(currentUserHelper).getCurrentUser();
        verify(categoryRepository).findAllByUser(user);
    }

    @Test
    void getByType_withExistingCategories_shouldReturnCategoryList() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findAllByTypeAndUser(CategoryType.EXPENSE, user))
                .thenReturn(List.of(foodCategory));

        CategoryResponse<List<CategoryByTypeData>> response = categoryService.getByType("expense");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category list fetched successfully", response.getMessage());
        assertEquals(1, response.getData().size());

        CategoryByTypeData data = response.getData().get(0);
        assertEquals(20L, data.getId());
        assertEquals("Food", data.getName());
        assertEquals("food-icon", data.getIcon());
        assertEquals("food.png", data.getEmojiUrl());

        verify(currentUserHelper).getCurrentUser();
        verify(categoryRepository).findAllByTypeAndUser(CategoryType.EXPENSE, user);
    }

    @Test
    void getByType_whenNoCategoriesFound_shouldReturnEmptyList() {
        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.findAllByTypeAndUser(CategoryType.INCOME, user))
                .thenReturn(List.of());

        CategoryResponse<List<CategoryByTypeData>> response = categoryService.getByType("INCOME");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("No categories found for the given type", response.getMessage());
        assertTrue(response.getData().isEmpty());

        verify(categoryRepository).findAllByTypeAndUser(CategoryType.INCOME, user);
    }

    @Test
    void getByType_withInvalidType_shouldThrowException() {
        assertThrows(
                BusinessValidationException.class,
                () -> categoryService.getByType("saving")
        );

        verify(currentUserHelper, never()).getCurrentUser();
        verify(categoryRepository, never()).findAllByTypeAndUser(any(), any());
    }

    @Test
    void create_withNameAndNoEmoji_shouldCreateCategoryUsingIconName() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");
        request.setType("expense");
        request.setEmoji("");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Food"))
                .thenReturn(false);
        when(categoryIconRepository.findByCategoryName("Food"))
                .thenReturn(Optional.of(foodIcon));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(20L);
                    return category;
                });

        CategoryResponse<CreateCategoryData> response = categoryService.create(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Category created successfully", response.getMessage());
        assertEquals(20L, response.getData().getId());
        assertEquals("Food", response.getData().getName());
        assertEquals("EXPENSE", response.getData().getType());
        assertEquals("food-icon", response.getData().getIcon());
        assertEquals("food.png", response.getData().getIconUrl());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());

        Category savedCategory = categoryCaptor.getValue();
        assertSame(user, savedCategory.getUser());
        assertSame(foodIcon, savedCategory.getCategoryIcon());
        assertEquals(CategoryType.EXPENSE, savedCategory.getType());
    }

    @Test
    void create_withEmoji_shouldCreateCategoryUsingEmoji() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Salary");
        request.setType("income");
        request.setEmoji("money-icon");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Salary"))
                .thenReturn(false);
        when(categoryIconRepository.findByEmoji("money-icon"))
                .thenReturn(Optional.of(salaryIcon));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(10L);
                    return category;
                });

        CategoryResponse<CreateCategoryData> response = categoryService.create(request);

        assertTrue(response.isSuccess());
        assertEquals("Category created successfully", response.getMessage());
        assertEquals(10L, response.getData().getId());
        assertEquals("Salary", response.getData().getName());
        assertEquals("INCOME", response.getData().getType());

        verify(categoryIconRepository).findByEmoji("money-icon");
        verify(categoryIconRepository, never()).findByCategoryName("Salary");
    }

    @Test
    void create_withExistingCategoryName_shouldThrowException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");
        request.setType("expense");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Food"))
                .thenReturn(true);

        assertThrows(
                BusinessConflictException.class,
                () -> categoryService.create(request)
        );

        verify(categoryIconRepository, never()).findByCategoryName(any());
        verify(categoryIconRepository, never()).findByEmoji(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_whenIconNameNotFound_shouldThrowException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Travel");
        request.setType("expense");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Travel"))
                .thenReturn(false);
        when(categoryIconRepository.findByCategoryName("Travel"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.create(request)
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_whenEmojiNotFound_shouldThrowException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Travel");
        request.setType("expense");
        request.setEmoji("plane-icon");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Travel"))
                .thenReturn(false);
        when(categoryIconRepository.findByEmoji("plane-icon"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> categoryService.create(request)
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_withInvalidType_shouldThrowException() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Food");
        request.setType("unknown");
        request.setEmoji("food-icon");

        when(currentUserHelper.getCurrentUser())
                .thenReturn(user);
        when(categoryRepository.existsByUserAndCategoryIcon_CategoryName(user, "Food"))
                .thenReturn(false);
        when(categoryIconRepository.findByEmoji("food-icon"))
                .thenReturn(Optional.of(foodIcon));

        assertThrows(
                BusinessValidationException.class,
                () -> categoryService.create(request)
        );

        verify(categoryRepository, never()).save(any());
    }

    private CategoryIcon createIcon(Long id, String name, String emoji, String iconUrl) {
        CategoryIcon icon = new CategoryIcon();
        icon.setId(id);
        icon.setCategoryName(name);
        icon.setEmoji(emoji);
        icon.setIconUrl(iconUrl);
        return icon;
    }

    private Category createCategory(Long id, User user, CategoryIcon icon, CategoryType type) {
        Category category = new Category();
        category.setId(id);
        category.setUser(user);
        category.setCategoryIcon(icon);
        category.setType(type);
        return category;
    }
}
