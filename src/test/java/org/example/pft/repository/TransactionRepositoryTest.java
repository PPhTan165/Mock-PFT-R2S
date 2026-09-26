package org.example.pft.repository;

import org.example.pft.dto.transaction.HistoryData;
import org.example.pft.entity.Category;
import org.example.pft.entity.CategoryIcon;
import org.example.pft.entity.Transaction;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TransactionRepositoryTest {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    EntityManager entityManager;

    private User userA;
    private User userB;
    private Category userAFoodCategory;
    private Category userARentCategory;
    private Category userASalaryCategory;
    private Category userBFoodCategory;

    @BeforeEach
    void setup() {
        userA = persistUser("user-a@example.com");
        userB = persistUser("user-b@example.com");

        CategoryIcon foodIcon = persistIcon("Food", "food-icon", "food.png");
        CategoryIcon rentIcon = persistIcon("Rent", "rent-icon", "rent.png");
        CategoryIcon salaryIcon = persistIcon("Salary", "salary-icon", "salary.png");
        CategoryIcon userBFoodIcon = persistIcon("Food B", "food-b-icon", "food-b.png");

        userAFoodCategory = persistCategory(userA, foodIcon, CategoryType.EXPENSE);
        userARentCategory = persistCategory(userA, rentIcon, CategoryType.EXPENSE);
        userASalaryCategory = persistCategory(userA, salaryIcon, CategoryType.INCOME);
        userBFoodCategory = persistCategory(userB, userBFoodIcon, CategoryType.EXPENSE);

        persistTransaction(userA, userAFoodCategory, "10.00", LocalDate.of(2026, 9, 5));
        persistTransaction(userA, userARentCategory, "20.00", LocalDate.of(2026, 9, 6));
        persistTransaction(userA, userAFoodCategory, "30.00", LocalDate.of(2026, 9, 20));
        persistTransaction(userA, userAFoodCategory, "40.00", LocalDate.of(2026, 8, 31));
        persistTransaction(userA, userASalaryCategory, "100.00", LocalDate.of(2026, 9, 7));
        persistTransaction(userB, userBFoodCategory, "999.00", LocalDate.of(2026, 9, 25));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void showHistory_withoutCategoryFilter_shouldReturnOnlyCurrentUsersTransactions() {
        List<HistoryData> history = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                CategoryType.EXPENSE,
                PageRequest.of(0, 10)
        );

        assertEquals(3, history.size());
        assertHistoryRow(history.get(0), userAFoodCategory.getId(), "Food", "30.00", LocalDate.of(2026, 9, 20));
        assertHistoryRow(history.get(1), userARentCategory.getId(), "Rent", "20.00", LocalDate.of(2026, 9, 6));
        assertHistoryRow(history.get(2), userAFoodCategory.getId(), "Food", "10.00", LocalDate.of(2026, 9, 5));
    }

    @Test
    void showHistory_withOwnedCategory_shouldReturnOnlyMatchingTransactions() {
        List<HistoryData> history = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                userAFoodCategory.getId(),
                CategoryType.EXPENSE,
                PageRequest.of(0, 10)
        );

        assertEquals(2, history.size());
        assertHistoryRow(history.get(0), userAFoodCategory.getId(), "Food", "30.00", LocalDate.of(2026, 9, 20));
        assertHistoryRow(history.get(1), userAFoodCategory.getId(), "Food", "10.00", LocalDate.of(2026, 9, 5));
    }

    @Test
    void showHistory_withOtherUsersCategory_shouldReturnEmptyForCurrentUser() {
        List<HistoryData> history = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                userBFoodCategory.getId(),
                CategoryType.EXPENSE,
                PageRequest.of(0, 10)
        );

        assertTrue(history.isEmpty());
    }

    @Test
    void showHistory_withPagination_shouldPageAfterOwnershipFilter() {
        List<HistoryData> firstPage = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                CategoryType.EXPENSE,
                PageRequest.of(0, 2)
        );
        List<HistoryData> secondPage = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                null,
                CategoryType.EXPENSE,
                PageRequest.of(1, 2)
        );

        assertEquals(2, firstPage.size());
        assertHistoryRow(firstPage.get(0), userAFoodCategory.getId(), "Food", "30.00", LocalDate.of(2026, 9, 20));
        assertHistoryRow(firstPage.get(1), userARentCategory.getId(), "Rent", "20.00", LocalDate.of(2026, 9, 6));
        assertEquals(1, secondPage.size());
        assertHistoryRow(secondPage.get(0), userAFoodCategory.getId(), "Food", "10.00", LocalDate.of(2026, 9, 5));
    }

    @Test
    void showHistory_withDateAndCategoryFilters_shouldPreserveOwnership() {
        List<HistoryData> history = transactionRepository.showHistory(
                userA.getId(),
                LocalDate.of(2026, 9, 6),
                LocalDate.of(2026, 9, 30),
                userAFoodCategory.getId(),
                CategoryType.EXPENSE,
                PageRequest.of(0, 10)
        );

        assertEquals(1, history.size());
        assertHistoryRow(history.get(0), userAFoodCategory.getId(), "Food", "30.00", LocalDate.of(2026, 9, 20));
    }

    @Test
    void findByIdAndUser_withOtherUsersCategory_shouldReturnEmpty() {
        Optional<Category> ownedCategory = categoryRepository.findByIdAndUser(userAFoodCategory.getId(), userA);
        Optional<Category> otherUsersCategory = categoryRepository.findByIdAndUser(userBFoodCategory.getId(), userA);

        assertTrue(ownedCategory.isPresent());
        assertEquals(userAFoodCategory.getId(), ownedCategory.get().getId());
        assertFalse(otherUsersCategory.isPresent());
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

    private Transaction persistTransaction(User user, Category category, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDate(date);
        entityManager.persist(transaction);
        return transaction;
    }

    private void assertHistoryRow(
            HistoryData historyData,
            Long categoryId,
            String categoryName,
            String amount,
            LocalDate date
    ) {
        assertEquals(categoryId, historyData.getId());
        assertEquals(categoryName, historyData.getCategory().getName());
        assertEquals(CategoryType.EXPENSE, historyData.getCategory().getType());
        assertEquals(0, historyData.getAmount().compareTo(new BigDecimal(amount)));
        assertEquals(date, historyData.getDate());
    }
}
