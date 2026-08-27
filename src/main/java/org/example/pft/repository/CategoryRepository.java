package org.example.pft.repository;

import org.example.pft.entity.Category;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByTypeAndUser(CategoryType type,User user);
    List<Category> findAllByUser(User user);
    boolean existsByUserAndCategoryIcon_CategoryName(User user, String categoryName);

}
