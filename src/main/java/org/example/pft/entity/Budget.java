package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets")
@Data
public class Budget {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @ManyToOne
    private Category category;

    @ManyToOne
    private User user;

    @Column(columnDefinition = "TINYINT")
    private Byte month;

    @Column(columnDefinition = "SMALLINT")
    private Short year;

    private LocalDateTime createdAt = LocalDateTime.now();
}
