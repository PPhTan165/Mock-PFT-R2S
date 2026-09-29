package org.example.pft.repository;

import jakarta.persistence.EntityManager;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CategoryRepositoryIsolationTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    EntityManager entityManager;

    private User userA;
    private User userB;
    private User userWithoutCategories;
    private User userWithoutIncomeCategories;

    private Category userAFoodExpense;
    private Category userASalaryIncome;
    private Category userBFoodExpense;
    private Category userBSalaryIncome;
    private Category userBTravelExpense;

    @BeforeEach
    void setup() {
        userA = persistUser("category-user-a@example.com");
        userB = persistUser("category-user-b@example.com");
        userWithoutCategories = persistUser("category-empty@example.com");
        userWithoutIncomeCategories = persistUser("category-expense-only@example.com");

        CategoryIcon foodIcon = persistIcon("Food", "food-icon", "food.png");
        CategoryIcon salaryIcon = persistIcon("Salary", "salary-icon", "salary.png");
        CategoryIcon travelIcon = persistIcon("Travel", "travel-icon", "travel.png");

        userAFoodExpense = persistCategory(userA, foodIcon, CategoryType.EXPENSE);
        userASalaryIncome = persistCategory(userA, salaryIcon, CategoryType.INCOME);

        userBFoodExpense = persistCategory(userB, foodIcon, CategoryType.EXPENSE);
        userBSalaryIncome = persistCategory(userB, salaryIcon, CategoryType.INCOME);
        userBTravelExpense = persistCategory(userB, travelIcon, CategoryType.EXPENSE);

        persistCategory(userWithoutIncomeCategories, foodIcon, CategoryType.EXPENSE);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllByUser_withUserA_shouldReturnOnlyUserACategories() {
        List<Category> results = categoryRepository.findAllByUser(userA);

        assertCategoryIds(results, Set.of(userAFoodExpense.getId(), userASalaryIncome.getId()));
        assertTrue(results.stream().allMatch(category -> category.getUser().getId().equals(userA.getId())));
    }

    @Test
    void findAllByUser_withUserB_shouldReturnOnlyUserBCategories() {
        List<Category> results = categoryRepository.findAllByUser(userB);

        assertCategoryIds(
                results,
                Set.of(userBFoodExpense.getId(), userBSalaryIncome.getId(), userBTravelExpense.getId())
        );
        assertTrue(results.stream().allMatch(category -> category.getUser().getId().equals(userB.getId())));
    }

    @Test
    void findAllByUser_withUserWithoutCategories_shouldReturnEmptyList() {
        List<Category> results = categoryRepository.findAllByUser(userWithoutCategories);

        assertTrue(results.isEmpty());
    }

    @Test
    void findAllByTypeAndUser_withUserAExpense_shouldReturnOnlyUserAExpenseCategories() {
        List<Category> results = categoryRepository.findAllByTypeAndUser(CategoryType.EXPENSE, userA);

        assertCategoryIds(results, Set.of(userAFoodExpense.getId()));
        assertAllMatchOwnerAndType(results, userA, CategoryType.EXPENSE);
    }

    @Test
    void findAllByTypeAndUser_withUserAIncome_shouldReturnOnlyUserAIncomeCategories() {
        List<Category> results = categoryRepository.findAllByTypeAndUser(CategoryType.INCOME, userA);

        assertCategoryIds(results, Set.of(userASalaryIncome.getId()));
        assertAllMatchOwnerAndType(results, userA, CategoryType.INCOME);
    }

    @Test
    void findAllByTypeAndUser_withUserBExpense_shouldReturnOnlyUserBExpenseCategories() {
        List<Category> results = categoryRepository.findAllByTypeAndUser(CategoryType.EXPENSE, userB);

        assertCategoryIds(results, Set.of(userBFoodExpense.getId(), userBTravelExpense.getId()));
        assertAllMatchOwnerAndType(results, userB, CategoryType.EXPENSE);
    }

    @Test
    void findAllByTypeAndUser_withNoMatchingType_shouldReturnEmptyList() {
        List<Category> results = categoryRepository.findAllByTypeAndUser(
                CategoryType.INCOME,
                userWithoutIncomeCategories
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void existsByUserAndCategoryIconCategoryName_withMatchForUserA_shouldReturnTrue() {
        boolean exists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userA, "Food");

        assertTrue(exists);
    }

    @Test
    void existsByUserAndCategoryIconCategoryName_withMatchOnlyForUserB_shouldReturnFalseForUserA() {
        boolean exists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userA, "Travel");

        assertFalse(exists);
    }

    @Test
    void existsByUserAndCategoryIconCategoryName_withNoMatchingCategoryName_shouldReturnFalse() {
        boolean userAExists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userA, "Utilities");
        boolean userBExists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userB, "Utilities");

        assertAll(
                () -> assertFalse(userAExists),
                () -> assertFalse(userBExists)
        );
    }

    @Test
    void existsByUserAndCategoryIconCategoryName_withSameNameForBothUsers_shouldReturnTrueForEachUser() {
        boolean userAExists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userA, "Salary");
        boolean userBExists = categoryRepository.existsByUserAndCategoryIcon_CategoryName(userB, "Salary");

        assertAll(
                () -> assertTrue(userAExists),
                () -> assertTrue(userBExists)
        );
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(email);
        user.setPassword("encoded-password");
        entityManager.persist(user);
        return user;
    }

    private CategoryIcon persistIcon(String name, String emoji, String iconUrl) {
        CategoryIcon icon = new CategoryIcon();
        icon.setCategoryName(name);
        icon.setEmoji(emoji);
        icon.setIconUrl(iconUrl);
        entityManager.persist(icon);
        return icon;
    }

    private Category persistCategory(User user, CategoryIcon icon, CategoryType type) {
        Category category = new Category();
        category.setUser(user);
        category.setCategoryIcon(icon);
        category.setType(type);
        entityManager.persist(category);
        return category;
    }

    private void assertCategoryIds(List<Category> categories, Set<Long> expectedIds) {
        Set<Long> actualIds = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
        assertEquals(expectedIds, actualIds);
        assertEquals(expectedIds.size(), categories.size());
    }

    private void assertAllMatchOwnerAndType(List<Category> categories, User expectedUser, CategoryType expectedType) {
        assertAll(
                () -> assertTrue(categories.stream()
                        .allMatch(category -> category.getUser().getId().equals(expectedUser.getId()))),
                () -> assertTrue(categories.stream()
                        .allMatch(category -> category.getType().equals(expectedType)))
        );
    }
}
