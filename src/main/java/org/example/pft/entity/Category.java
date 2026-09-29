package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.example.pft.enums.CategoryType;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private CategoryIcon categoryIcon;

    @Enumerated(EnumType.STRING)
    private CategoryType type;

    private LocalDateTime createdAt = LocalDateTime.now();

}
