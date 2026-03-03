package com.liquilabs.vankoo.iam.domain.model.entities;

import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleId;
import com.liquilabs.vankoo.iam.domain.model.valueobjects.RoleName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.data.domain.Persistable;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@With
public class Role implements Persistable<RoleId> {

    @EmbeddedId
    private RoleId id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RoleName name;

    @Transient // No se guarda en la BD
    private boolean isNew = true;

    public Role(RoleName name) {
        this.id = new RoleId(); // Genera un nuevo ID automáticamente
        this.name = name;
    }

    public String getStringName() {
        return name.name();
    }

    public static Role getDefaultRole() {
        return new Role(RoleName.ROLE_USER);
    }

    public static Role toRoleFromName(String name) {
        return new Role(RoleName.valueOf(name));
    }

    public static List<Role> validateRoleSet(List<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(getDefaultRole());
        }
        return roles;
    }

    // Al cargar desde la BD, JPA usa el constructor o setters.
    // Podemos usar un callback de JPA para marcar que ya no es nuevo.
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public RoleId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
