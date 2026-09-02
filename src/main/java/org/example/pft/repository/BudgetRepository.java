package org.example.pft.repository;

import org.example.pft.entity.Budget;
import org.example.pft.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget,Long> {
    boolean existsByCategoryAndMonthAndYear(Category category, Byte month, Short year);
    Optional<Budget> findByCategoryAndMonthAndYear(Category category, Byte month, Short year);
    List<Budget> findByMonthAndYear(Byte month, Short year);
}
