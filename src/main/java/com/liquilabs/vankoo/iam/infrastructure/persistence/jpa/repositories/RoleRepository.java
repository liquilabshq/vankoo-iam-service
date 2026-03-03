package com.liquilabs.vankoo.iam.infrastructure.persistence.jpa.repositories;

import com.liquilabs.vankoo.iam.domain.model.entities.Role;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleId;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, RoleId> {
    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
