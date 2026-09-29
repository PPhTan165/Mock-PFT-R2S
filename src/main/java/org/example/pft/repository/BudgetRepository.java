package org.example.pft.repository;

import org.example.pft.entity.Budget;
import org.example.pft.entity.Category;
import org.example.pft.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget,Long> {
    Optional<Budget> findByIdAndUser(Long id, User user);
    Optional<Budget> findByUserAndCategoryAndMonthAndYear(User user, Category category, Byte month, Short year);
    List<Budget> findByUserAndMonthAndYear(User user, Byte month, Short year);
}
