package com.entropyteam.entropay.users.auth.services;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;

/**
 * Resolves a user's email from a Cognito access token via the Cognito userInfo endpoint.
 *
 * <p>Cognito access tokens do not carry an {@code email} claim — only id tokens do. MCP
 * clients (e.g. Claude custom connectors) authenticate with access tokens, so the email
 * has to be fetched from {@code /oauth2/userInfo}, which accepts the access token.
 */
@Service
public class CognitoUserInfoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CognitoUserInfoService.class);

    private final RestClient restClient;

    public CognitoUserInfoService(@Value("${mcp.auth-gateway.cognito-domain}") String cognitoDomain) {
        this.restClient = RestClient.builder().baseUrl(cognitoDomain).build();
    }

    /**
     * Fetches the {@code email} of the user the given Cognito access token belongs to.
     *
     * @param accessToken a valid Cognito access token
     * @return the user's email address
     * @throws AuthException if userInfo cannot be reached or returns no email
     */
    public String fetchEmail(String accessToken) {
        try {
            Map<?, ?> userInfo = restClient.get()
                    .uri("/oauth2/userInfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);
            Object email = userInfo != null ? userInfo.get("email") : null;
            if (email == null) {
                throw new AuthException("Cognito userInfo did not return an email");
            }
            return email.toString();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to resolve email from Cognito userInfo", e);
            throw new AuthException("Could not resolve user email from access token");
        }
    }
}
