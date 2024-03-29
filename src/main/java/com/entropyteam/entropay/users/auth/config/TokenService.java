package com.entropyteam.entropay.users.auth.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
public class TokenService {

    private static final String COGNITO_CLAIMS_USERNAME_KEY = "username";
    private static final String COGNITO_JWT_CLAIMS_USERNAME_KEY = "cognito:username";
    private static final String COGNITO_CLAIMS_ROLE_KEY = "custom:role";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, Collection<String>> getRoles(Map<String, Object> tokenClaims) {
        try {
            return MAPPER.readValue((String) tokenClaims.get(COGNITO_CLAIMS_ROLE_KEY), Map.class);
        } catch (JsonProcessingException e) {
            throw new AuthException("Error reading roles from token");
        }
    }

    public String getUsername(Map<String, Object> tokenClaims) {
        return (String) tokenClaims.getOrDefault(COGNITO_CLAIMS_USERNAME_KEY,
                tokenClaims.get(COGNITO_JWT_CLAIMS_USERNAME_KEY));
    }

    public void validateTokenRoles(Map<String, Object> tokenClaims, Map<UUID, Set<String>> rolesByTenant) {
        boolean validRoles = getRoles(tokenClaims).entrySet().stream().allMatch(
                r -> rolesByTenant.getOrDefault(UUID.fromString(r.getKey()), Collections.emptySet())
                        .containsAll(r.getValue()));
        if (!validRoles) {
            throw new AuthException("Invalid roles for user " + getUsername(tokenClaims));
        }
    }
}
