package com.entropyteam.entropay.users.auth.models;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import com.entropyteam.entropay.users.auth.common.BaseEntity;

@Entity
@Table(name = "role")
public class Role extends BaseEntity {

    private String roleName;

    @OneToMany(mappedBy = "role")
    Set<UserTenant> userTenants;

    public Role() {
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
