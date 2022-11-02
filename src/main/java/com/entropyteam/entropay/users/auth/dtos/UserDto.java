package com.entropyteam.entropay.users.auth.dtos;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.entropyteam.entropay.users.auth.models.User;

public record UserDto(UUID id, String username, String firstName, String lastName, String email,
                      Map<UUID, Set<String>> rolesByTenant) {

    public UserDto(User user) {
        this(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getUserTenants().stream().collect(
                        Collectors.groupingBy(userTenant -> userTenant.getTenant().getId(),
                                Collectors.mapping(userTenant -> userTenant.getRole().getRoleName(),
                                        Collectors.toSet()))));
    }
}
