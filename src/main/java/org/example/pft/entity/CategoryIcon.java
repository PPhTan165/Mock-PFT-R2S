package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "category_icons")
@Data
public class CategoryIcon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;
    private String emoji;
    private String iconUrl;
    private LocalDateTime createdAt;


}
