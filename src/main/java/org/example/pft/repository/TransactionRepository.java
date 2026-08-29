package org.example.pft.repository;

import org.example.pft.dto.dashboard.PieChartData;
import org.example.pft.dto.dashboard.RecentTransData;
import org.example.pft.entity.Transaction;
import org.example.pft.enums.CategoryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    @Query("""
            select new org.example.pft.dto.dashboard.PieChartData(
                ci.categoryName as category,
                sum(t.amount),
                c.type
            )
            from Transaction t
            join t.category c
            join c.categoryIcon ci
            where t.user.id = :userId
              and month(t.date) = :month
              and year(t.date) = :year
            group by ci.categoryName, c.type
            order by sum(t.amount)
            """)
    List<PieChartData> findPieChartData(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year

    );

    @Query("""
            select new org.example.pft.dto.dashboard.RecentTransData(
                t.id,
                ci.categoryName,
                ci.emoji,
                  case 
                       when c.type = 'EXPENSE' then -t.amount
                       else t.amount
                   end as amount,
                t.date,
                c.type
            )
            from Transaction t
            join t.category c
            join c.categoryIcon ci
            where t.user.id = :userId
              and month(t.date) = :month
              and year(t.date) = :year
            order by t.date desc
            """)
    List<RecentTransData> findRecentTransData(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            Pageable pageable
    );

    @Query("""
            select sum(t.amount)
            from Transaction t
            join t.category c
            where t.user.id = :userId
              and month(t.date) = :month
              and year(t.date) = :year
              and c.type = :type
            """)
    BigDecimal getTotalByType(
            @Param("userId") Long userId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("type") CategoryType type
    );
}
