package org.example.pft.repository;

import org.example.pft.dto.report.ReportCategory;
import org.example.pft.entity.Category;
import org.example.pft.entity.User;
import org.example.pft.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByTypeAndUser(CategoryType type,User user);
    List<Category> findAllByUser(User user);
    boolean existsByUserAndCategoryIcon_CategoryName(User user, String categoryName);

    @Query("""
            select new org.example.pft.dto.report.ReportCategory(
                ci.categoryName,
                sum(t.amount)
            )
            from Transaction t
            join t.category c
            join c.categoryIcon ci
            where t.user.id = :userId
              and c.type = :type
              and month(t.date) = :month
              and year(t.date) = :year
            group by ci.categoryName
            order by sum(t.amount) desc
    """)
    List<ReportCategory> findReportCategoryData(
            @Param("type") CategoryType type,
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year
    );

}
