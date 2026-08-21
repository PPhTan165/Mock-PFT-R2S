package org.example.pft.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
}
