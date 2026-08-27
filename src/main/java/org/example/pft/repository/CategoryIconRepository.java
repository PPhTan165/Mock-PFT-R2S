package org.example.pft.repository;

import org.example.pft.entity.CategoryIcon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryIconRepository extends JpaRepository<CategoryIcon, Long> {
    Optional<CategoryIcon> findById(Long id);
    Optional<CategoryIcon> findByEmoji(String emoji);
    Optional<CategoryIcon> findByCategoryName(String categoryName);
}
