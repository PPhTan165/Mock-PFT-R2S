package org.example.pft.repository;

import jakarta.persistence.EntityManager;
import org.example.pft.entity.Budget;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class BudgetRepositoryIsolationTest {

    private static final Byte REPORT_MONTH = 9;
    private static final Short REPORT_YEAR = 2026;

    @Autowired
    BudgetRepository budgetRepository;

    @Autowired
    EntityManager entityManager;

    private User userA;
    private User userB;
    private User userWithoutBudgets;

    private Category userAFoodCategory;
    private Category userARentCategory;
    private Category userAUtilitiesCategory;
    private Category userBFoodCategory;
    private Category userBRentCategory;

    private Budget userAFoodSeptemberBudget;
    private Budget userARentSeptemberBudget;
    private Budget userBFoodSeptemberBudget;
    private Budget userBRentSeptemberBudget;

    @BeforeEach
    void setup() {
        userA = persistUser("budget-user-a@example.com");
        userB = persistUser("budget-user-b@example.com");
        userWithoutBudgets = persistUser("budget-empty@example.com");

        userAFoodCategory = persistCategory(userA, "Food", "food-a-icon", "food-a.png");
        userARentCategory = persistCategory(userA, "Rent", "rent-a-icon", "rent-a.png");
        userAUtilitiesCategory = persistCategory(userA, "Utilities", "utilities-a-icon", "utilities-a.png");
        userBFoodCategory = persistCategory(userB, "Food", "food-b-icon", "food-b.png");
        userBRentCategory = persistCategory(userB, "Rent", "rent-b-icon", "rent-b.png");

        userAFoodSeptemberBudget = persistBudget(userA, userAFoodCategory, "100.00", REPORT_MONTH, REPORT_YEAR);
        userARentSeptemberBudget = persistBudget(userA, userARentCategory, "200.00", REPORT_MONTH, REPORT_YEAR);
        persistBudget(userA, userAFoodCategory, "300.00", (byte) 8, REPORT_YEAR);
        persistBudget(userA, userAFoodCategory, "400.00", REPORT_MONTH, (short) 2025);
        userBFoodSeptemberBudget = persistBudget(userB, userBFoodCategory, "500.00", REPORT_MONTH, REPORT_YEAR);
        userBRentSeptemberBudget = persistBudget(userB, userBRentCategory, "600.00", REPORT_MONTH, REPORT_YEAR);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByIdAndUser_withCorrectOwner_shouldReturnExactBudget() {
        Optional<Budget> result = budgetRepository.findByIdAndUser(userAFoodSeptemberBudget.getId(), userA);

        assertTrue(result.isPresent());
        Budget budget = result.get();
        assertBudgetIdentity(budget, userAFoodSeptemberBudget, userA, userAFoodCategory, REPORT_MONTH, REPORT_YEAR);
    }

    @Test
    void findByIdAndUser_withDifferentOwner_shouldReturnEmpty() {
        Optional<Budget> result = budgetRepository.findByIdAndUser(userAFoodSeptemberBudget.getId(), userB);

        assertFalse(result.isPresent());
    }

    @Test
    void findByIdAndUser_withNonexistentId_shouldReturnEmpty() {
        Optional<Budget> result = budgetRepository.findByIdAndUser(nextBudgetId(), userA);

        assertFalse(result.isPresent());
    }

    @Test
    void findByUserAndMonthAndYear_withUserA_shouldReturnOnlyUserAMatchingPeriodBudgets() {
        List<Budget> results = budgetRepository.findByUserAndMonthAndYear(userA, REPORT_MONTH, REPORT_YEAR);

        assertBudgetIds(results, Set.of(userAFoodSeptemberBudget.getId(), userARentSeptemberBudget.getId()));
        assertTrue(results.stream().allMatch(budget -> budget.getUser().getId().equals(userA.getId())));
        assertTrue(results.stream().allMatch(budget -> budget.getMonth().equals(REPORT_MONTH)));
        assertTrue(results.stream().allMatch(budget -> budget.getYear().equals(REPORT_YEAR)));
    }

    @Test
    void findByUserAndMonthAndYear_withUserB_shouldReturnOnlyUserBMatchingPeriodBudgets() {
        List<Budget> results = budgetRepository.findByUserAndMonthAndYear(userB, REPORT_MONTH, REPORT_YEAR);

        assertBudgetIds(results, Set.of(userBFoodSeptemberBudget.getId(), userBRentSeptemberBudget.getId()));
        assertTrue(results.stream().allMatch(budget -> budget.getUser().getId().equals(userB.getId())));
        assertTrue(results.stream().allMatch(budget -> budget.getMonth().equals(REPORT_MONTH)));
        assertTrue(results.stream().allMatch(budget -> budget.getYear().equals(REPORT_YEAR)));
    }

    @Test
    void findByUserAndMonthAndYear_withNoMatchingRecords_shouldReturnEmptyList() {
        List<Budget> results = budgetRepository.findByUserAndMonthAndYear(userWithoutBudgets, REPORT_MONTH, REPORT_YEAR);

        assertTrue(results.isEmpty());
    }

    @Test
    void findByUserAndCategoryAndMonthAndYear_withMatchingUserAndCategory_shouldReturnExactBudget() {
        Optional<Budget> result = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userA,
                userAFoodCategory,
                REPORT_MONTH,
                REPORT_YEAR
        );

        assertTrue(result.isPresent());
        assertBudgetIdentity(result.get(), userAFoodSeptemberBudget, userA, userAFoodCategory, REPORT_MONTH, REPORT_YEAR);
    }

    @Test
    void findByUserAndCategoryAndMonthAndYear_withWrongUser_shouldNotExposeOtherUsersBudget() {
        Optional<Budget> result = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userB,
                userAFoodCategory,
                REPORT_MONTH,
                REPORT_YEAR
        );

        assertFalse(result.isPresent());
    }

    @Test
    void findByUserAndCategoryAndMonthAndYear_withDifferentCategory_shouldReturnEmpty() {
        Optional<Budget> result = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userA,
                userAUtilitiesCategory,
                REPORT_MONTH,
                REPORT_YEAR
        );

        assertFalse(result.isPresent());
    }

    @Test
    void findByUserAndCategoryAndMonthAndYear_withDifferentMonthOrYear_shouldReturnEmpty() {
        Optional<Budget> differentMonth = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userA,
                userAFoodCategory,
                (byte) 10,
                REPORT_YEAR
        );
        Optional<Budget> differentYear = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userA,
                userAFoodCategory,
                REPORT_MONTH,
                (short) 2024
        );

        assertAll(
                () -> assertFalse(differentMonth.isPresent()),
                () -> assertFalse(differentYear.isPresent())
        );
    }

    @Test
    void findByUserAndCategoryAndMonthAndYear_withNoMatchingRecord_shouldReturnEmpty() {
        Optional<Budget> result = budgetRepository.findByUserAndCategoryAndMonthAndYear(
                userWithoutBudgets,
                userBFoodCategory,
                REPORT_MONTH,
                REPORT_YEAR
        );

        assertFalse(result.isPresent());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(email);
        user.setPassword("encoded-password");
        entityManager.persist(user);
        return user;
    }

    private Category persistCategory(User user, String name, String emoji, String iconUrl) {
        CategoryIcon icon = new CategoryIcon();
        icon.setCategoryName(name);
        icon.setEmoji(emoji);
        icon.setIconUrl(iconUrl);
        entityManager.persist(icon);

        Category category = new Category();
        category.setUser(user);
        category.setCategoryIcon(icon);
        category.setType(CategoryType.EXPENSE);
        entityManager.persist(category);
        return category;
    }

    private Budget persistBudget(User user, Category category, String amount, Byte month, Short year) {
        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setAmount(new BigDecimal(amount));
        budget.setMonth(month);
        budget.setYear(year);
        entityManager.persist(budget);
        return budget;
    }

    private Long nextBudgetId() {
        return entityManager.createQuery("select coalesce(max(b.id), 0) + 1 from Budget b", Long.class)
                .getSingleResult();
    }

    private void assertBudgetIds(List<Budget> budgets, Set<Long> expectedIds) {
        Set<Long> actualIds = budgets.stream()
                .map(Budget::getId)
                .collect(Collectors.toSet());
        assertEquals(expectedIds, actualIds);
        assertEquals(expectedIds.size(), budgets.size());
    }

    private void assertBudgetIdentity(
            Budget actual,
            Budget expected,
            User expectedUser,
            Category expectedCategory,
            Byte expectedMonth,
            Short expectedYear
    ) {
        assertAll(
                () -> assertEquals(expected.getId(), actual.getId()),
                () -> assertEquals(expectedUser.getId(), actual.getUser().getId()),
                () -> assertEquals(expectedCategory.getId(), actual.getCategory().getId()),
                () -> assertEquals(expectedMonth, actual.getMonth()),
                () -> assertEquals(expectedYear, actual.getYear())
        );
    }
}
