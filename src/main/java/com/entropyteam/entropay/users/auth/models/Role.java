package com.entropyteam.entropay.users.auth.models;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.entropyteam.entropay.users.auth.common.BaseEntity;

@Entity
@Table(name = "role")
public class Role extends BaseEntity {

    private String roleName;

    @OneToMany(mappedBy = "role")
    private Set<UserTenant> userTenants;

    public Role() {
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
