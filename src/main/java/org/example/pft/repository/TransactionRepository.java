package org.example.pft.repository;

import org.example.pft.dto.dashboard.PieChartData;
import org.example.pft.dto.dashboard.RecentTransData;
import org.example.pft.dto.transaction.HistoryData;
import org.example.pft.entity.Transaction;
import org.example.pft.enums.CategoryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    @Query("""
            select new org.example.pft.dto.transaction.HistoryData(
                c.id,
                ci.categoryName,
                ci.emoji,
                c.type,
                t.amount,
                t.date
            )
            from Transaction t
                join t.category c
                join c.categoryIcon ci
                where t.user.id = :userId
                    and t.date between :startDate and :endDate
                    and (:categoryId is null or c.id = :categoryId)
                    and c.type = :type
                order by t.date desc, t.id desc
                
""")
    List<HistoryData> showHistory(
            @Param("userId") Long userId,
            @Param("startDate")LocalDate startDate,
            @Param("endDate")LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("type") CategoryType type,
            Pageable pageable
            );

}
