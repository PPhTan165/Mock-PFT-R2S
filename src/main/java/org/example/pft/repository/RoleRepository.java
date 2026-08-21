package org.example.pft.repository;

import org.example.pft.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface RoleRepository extends JpaRepository<Long, Role> {
    Optional<Role> findByName(String name);
}
