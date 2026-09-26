package org.example.pft.repository;

import jakarta.persistence.EntityManager;
import org.example.pft.dto.report.category.ReportCategory;
import org.example.pft.dto.report.summary.TopExpenses;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ReportRepositoryIsolationTest {

    private static final Integer REPORT_MONTH = 9;
    private static final Integer REPORT_YEAR = 2026;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    EntityManager entityManager;

    private User userA;
    private User userB;
    private User userWithoutTransactions;

    @BeforeEach
    void setup() {
        userA = persistUser("report-user-a@example.com");
        userB = persistUser("report-user-b@example.com");
        userWithoutTransactions = persistUser("report-empty@example.com");

        Category userAFood = persistCategory(userA, "A Food", "a-food", "a-food.png", CategoryType.EXPENSE);
        Category userARent = persistCategory(userA, "A Rent", "a-rent", "a-rent.png", CategoryType.EXPENSE);
        Category userAUtilities = persistCategory(userA, "A Utilities", "a-utilities", "a-utilities.png", CategoryType.EXPENSE);
        Category userATravel = persistCategory(userA, "A Travel", "a-travel", "a-travel.png", CategoryType.EXPENSE);
        Category userASalary = persistCategory(userA, "A Salary", "a-salary", "a-salary.png", CategoryType.INCOME);

        Category userBFood = persistCategory(userB, "B Food", "b-food", "b-food.png", CategoryType.EXPENSE);
        Category userBRent = persistCategory(userB, "B Rent", "b-rent", "b-rent.png", CategoryType.EXPENSE);
        Category userBSalary = persistCategory(userB, "B Salary", "b-salary", "b-salary.png", CategoryType.INCOME);
        Category userBLeakedCategory = persistCategory(userB, "B Leaked", "b-leaked", "b-leaked.png", CategoryType.EXPENSE);

        persistTransaction(userA, userAFood, "120.00", LocalDate.of(2026, 9, 5));
        persistTransaction(userA, userARent, "80.00", LocalDate.of(2026, 9, 6));
        persistTransaction(userA, userAUtilities, "60.00", LocalDate.of(2026, 9, 7));
        persistTransaction(userA, userATravel, "40.00", LocalDate.of(2026, 9, 8));
        persistTransaction(userA, userASalary, "1000.00", LocalDate.of(2026, 9, 9));
        persistTransaction(userA, userAFood, "500.00", LocalDate.of(2026, 8, 31));

        persistTransaction(userB, userBFood, "9999.00", LocalDate.of(2026, 9, 10));
        persistTransaction(userB, userBRent, "8000.00", LocalDate.of(2026, 9, 11));
        persistTransaction(userB, userBSalary, "2222.00", LocalDate.of(2026, 9, 12));

        persistTransaction(userA, userBLeakedCategory, "7000.00", LocalDate.of(2026, 9, 13));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getTotalByType_shouldAggregateOnlyTransactionsAndCategoriesOwnedByUser() {
        BigDecimal expense = transactionRepository.getTotalByType(
                userA.getId(),
                REPORT_MONTH,
                REPORT_YEAR,
                CategoryType.EXPENSE
        );
        BigDecimal income = transactionRepository.getTotalByType(
                userA.getId(),
                REPORT_MONTH,
                REPORT_YEAR,
                CategoryType.INCOME
        );
        BigDecimal previousMonthExpense = transactionRepository.getTotalByType(
                userA.getId(),
                8,
                REPORT_YEAR,
                CategoryType.EXPENSE
        );

        assertAmount("300.00", expense);
        assertAmount("1000.00", income);
        assertAmount("500.00", previousMonthExpense);
    }

    @Test
    void findReportCategoryData_shouldGroupOnlyCategoriesAndTransactionsOwnedByUser() {
        List<ReportCategory> categories = categoryRepository.findReportCategoryData(
                CategoryType.EXPENSE,
                userA.getId(),
                REPORT_MONTH,
                REPORT_YEAR
        );

        Map<String, BigDecimal> amountsByCategory = categories.stream()
                .collect(Collectors.toMap(ReportCategory::getCategory, ReportCategory::getAmount));

        assertEquals(4, categories.size());
        assertAmount("120.00", amountsByCategory.get("A Food"));
        assertAmount("80.00", amountsByCategory.get("A Rent"));
        assertAmount("60.00", amountsByCategory.get("A Utilities"));
        assertAmount("40.00", amountsByCategory.get("A Travel"));
        assertFalse(amountsByCategory.containsKey("B Food"));
        assertFalse(amountsByCategory.containsKey("B Leaked"));
    }

    @Test
    void showTopCategories_shouldExcludeOtherUsersExpensesBeforeApplyingLimit() {
        List<TopExpenses> topExpenses = transactionRepository.showTopCategories(
                userA.getId(),
                REPORT_MONTH,
                REPORT_YEAR,
                CategoryType.EXPENSE
        );

        assertEquals(3, topExpenses.size());
        assertEquals("A Food", topExpenses.get(0).getCategory());
        assertAmount("120.00", topExpenses.get(0).getAmount());
        assertEquals("A Rent", topExpenses.get(1).getCategory());
        assertAmount("80.00", topExpenses.get(1).getAmount());
        assertEquals("A Utilities", topExpenses.get(2).getCategory());
        assertAmount("60.00", topExpenses.get(2).getAmount());
        assertTrue(topExpenses.stream().noneMatch(item -> item.getCategory().startsWith("B ")));
    }

    @Test
    void reportQueries_whenUserHasNoTransactions_shouldReturnExistingEmptyOrNullResults() {
        BigDecimal expense = transactionRepository.getTotalByType(
                userWithoutTransactions.getId(),
                REPORT_MONTH,
                REPORT_YEAR,
                CategoryType.EXPENSE
        );
        List<ReportCategory> categories = categoryRepository.findReportCategoryData(
                CategoryType.EXPENSE,
                userWithoutTransactions.getId(),
                REPORT_MONTH,
                REPORT_YEAR
        );
        List<TopExpenses> topExpenses = transactionRepository.showTopCategories(
                userWithoutTransactions.getId(),
                REPORT_MONTH,
                REPORT_YEAR,
                CategoryType.EXPENSE
        );

        assertNull(expense);
        assertTrue(categories.isEmpty());
        assertTrue(topExpenses.isEmpty());
    }

    private User persistUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(email);
        user.setPassword("encoded-password");
        entityManager.persist(user);
        return user;
    }

    private Category persistCategory(
            User user,
            String name,
            String emoji,
            String iconUrl,
            CategoryType type
    ) {
        Category category = new Category();
        category.setUser(user);
        category.setCategoryIcon(persistIcon(name, emoji, iconUrl));
        category.setType(type);
        entityManager.persist(category);
        return category;
    }

    private CategoryIcon persistIcon(String name, String emoji, String iconUrl) {
        CategoryIcon icon = new CategoryIcon();
        icon.setCategoryName(name);
        icon.setEmoji(emoji);
        icon.setIconUrl(iconUrl);
        entityManager.persist(icon);
        return icon;
    }

    private void persistTransaction(User user, Category category, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(date);
        entityManager.persist(transaction);
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}
