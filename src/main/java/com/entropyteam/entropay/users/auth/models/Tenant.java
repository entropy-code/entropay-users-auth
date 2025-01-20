package com.entropyteam.entropay.users.auth.models;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import com.entropyteam.entropay.users.auth.common.BaseEntity;

@Entity
@Table(name = "tenant")
public class Tenant extends BaseEntity {

    private String name;

    private String displayName;

    @OneToMany(mappedBy = "tenant")
    private Set<UserTenant> userTenants;

    public Tenant() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<UserTenant> getUserTenants() {
        return userTenants;
    }

    public void setUserTenants(Set<UserTenant> userTenants) {
        this.userTenants = userTenants;
    }
}
