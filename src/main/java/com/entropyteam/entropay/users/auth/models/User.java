package com.entropyteam.entropay.users.auth.models;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import com.entropyteam.entropay.users.auth.common.BaseEntity;

@Entity
@Table(name = "[user]")
public class User extends BaseEntity {

    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String externalId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "user")
    Set<UserTenant> userTenants;

    public User() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Set<UserTenant> getUserTenants() {
        return userTenants;
    }

    public void setUserTenants(Set<UserTenant> userTenants) {
        this.userTenants = userTenants;
    }
}
