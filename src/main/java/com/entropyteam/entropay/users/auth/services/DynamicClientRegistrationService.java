package com.entropyteam.entropay.users.auth.services;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.entropyteam.entropay.users.auth.common.exceptions.ClientRegistrationException;
import com.entropyteam.entropay.users.auth.config.McpAuthGatewayProperties;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationRequestDto;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationResponseDto;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

/**
 * Implements OAuth 2.0 Dynamic Client Registration (RFC 7591) by creating an AWS Cognito
 * user pool app client on demand. This lets MCP clients (e.g. Claude custom connectors)
 * self-register without an operator pre-creating a Cognito app client.
 *
 * <p>Cognito does not natively support RFC 7591; this service is the bridge. The created
 * client is an authorization-code + PKCE OAuth2 client restricted to the redirect URIs the
 * caller requested (each validated against the configured allowlist).
 */
@Service
public class DynamicClientRegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicClientRegistrationService.class);
    private static final int COGNITO_CLIENT_NAME_MAX_LENGTH = 128;

    private final CognitoIdentityProviderClient cognitoClient;
    private final McpAuthGatewayProperties properties;

    public DynamicClientRegistrationService(CognitoIdentityProviderClient cognitoClient,
            McpAuthGatewayProperties properties) {
        this.cognitoClient = cognitoClient;
        this.properties = properties;
    }

    public ClientRegistrationResponseDto register(ClientRegistrationRequestDto request) {
        List<String> redirectUris = request.redirectUris();
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new ClientRegistrationException("invalid_client_metadata", "redirect_uris is required");
        }
        for (String uri : redirectUris) {
            if (!isAllowedRedirectUri(uri)) {
                throw new ClientRegistrationException("invalid_redirect_uri",
                        "redirect_uri not allowed: " + uri);
            }
        }

        String cognitoClientName = buildCognitoClientName(request.clientName());

        try {
            CreateUserPoolClientResponse response = cognitoClient.createUserPoolClient(
                    CreateUserPoolClientRequest.builder()
                            .userPoolId(properties.userPoolId())
                            .clientName(cognitoClientName)
                            .generateSecret(true)
                            .callbackURLs(redirectUris)
                            .allowedOAuthFlows(OAuthFlowType.CODE)
                            .allowedOAuthFlowsUserPoolClient(true)
                            .allowedOAuthScopes(properties.allowedScopes())
                            .supportedIdentityProviders(properties.identityProviders())
                            .explicitAuthFlows(ExplicitAuthFlowsType.ALLOW_REFRESH_TOKEN_AUTH)
                            .enableTokenRevocation(true)
                            .build());

            UserPoolClientType client = response.userPoolClient();
            LOGGER.info("Registered MCP OAuth client {} ({}) for redirect_uris {}",
                    client.clientId(), cognitoClientName, redirectUris);

            return new ClientRegistrationResponseDto(
                    client.clientId(),
                    client.clientSecret(),
                    Instant.now().getEpochSecond(),
                    0L,
                    cognitoClientName,
                    client.callbackURLs(),
                    List.of("authorization_code", "refresh_token"),
                    List.of("code"),
                    "client_secret_basic",
                    String.join(" ", properties.allowedScopes()));
        } catch (CognitoIdentityProviderException e) {
            String detail = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            LOGGER.error("Cognito rejected client registration: {}", detail, e);
            throw new ClientRegistrationException("invalid_client_metadata", "Cognito error: " + detail);
        }
    }

    private boolean isAllowedRedirectUri(String uri) {
        return uri != null && properties.allowedRedirectUris().stream().anyMatch(uri::startsWith);
    }

    private String buildCognitoClientName(String requestedName) {
        String base = (requestedName == null || requestedName.isBlank()) ? "mcp-client" : requestedName;
        String sanitized = base.replaceAll("[^a-zA-Z0-9-]", "-");
        String name = "mcp-" + sanitized + "-" + Instant.now().toEpochMilli();
        return name.length() > COGNITO_CLIENT_NAME_MAX_LENGTH
                ? name.substring(0, COGNITO_CLIENT_NAME_MAX_LENGTH)
                : name;
    }
}
