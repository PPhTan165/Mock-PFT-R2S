package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String note;
    private BigDecimal amount;

    private LocalDate date;
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    private User user;

    @ManyToOne
    private Category category;
}
