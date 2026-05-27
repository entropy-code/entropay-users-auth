package com.entropyteam.entropay.users.auth.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExplicitAuthFlowsType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.OAuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

/**
 * Implements OAuth 2.0 Dynamic Client Registration (RFC 7591) by creating an AWS Cognito
 * user pool app client on demand. This lets MCP clients (e.g. Claude custom connectors)
 * self-register without an operator pre-creating a Cognito app client.
 *
 * <p>Cognito does not natively support RFC 7591; this service is the bridge. The created
 * client is an authorization-code + PKCE OAuth2 client restricted to the redirect URIs the
 * caller requested (each validated against the configured allowlist).
 *
 * <p>Registration is idempotent by {@code redirect_uris}: if a previously registered
 * {@code mcp-*} app client already exists with the exact same callback URLs, it is reused
 * instead of creating a new one. This prevents the app-client count from growing
 * unbounded as the same MCP connector is registered from multiple installations.
 */
@Service
public class DynamicClientRegistrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicClientRegistrationService.class);
    private static final String MCP_CLIENT_NAME_PREFIX = "mcp-";
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

        // Idempotency: reuse an existing mcp-* client whose CallbackURLs match exactly.
        UserPoolClientType existing = findExistingMcpClient(redirectUris);
        if (existing != null) {
            LOGGER.info("Reusing existing MCP OAuth client {} ({}) for redirect_uris {}",
                    existing.clientId(), existing.clientName(), redirectUris);
            return toResponse(existing, existing.clientName());
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
            return toResponse(client, cognitoClientName);
        } catch (CognitoIdentityProviderException e) {
            String detail = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            LOGGER.error("Cognito rejected client registration: {}", detail, e);
            throw new ClientRegistrationException("invalid_client_metadata", "Cognito error: " + detail);
        }
    }

    /**
     * Looks up an existing {@code mcp-*} app client whose {@code CallbackURLs} match the
     * requested redirect_uris as a set. Returns {@code null} if there is no match, or if
     * the lookup itself fails (in which case the caller proceeds to create a new client).
     */
    private UserPoolClientType findExistingMcpClient(List<String> requestedUris) {
        Set<String> requested = new HashSet<>(requestedUris);
        try {
            String nextToken = null;
            do {
                ListUserPoolClientsRequest.Builder listBuilder = ListUserPoolClientsRequest.builder()
                        .userPoolId(properties.userPoolId())
                        .maxResults(60);
                if (nextToken != null) {
                    listBuilder.nextToken(nextToken);
                }
                ListUserPoolClientsResponse list = cognitoClient.listUserPoolClients(listBuilder.build());
                for (UserPoolClientDescription desc : list.userPoolClients()) {
                    String name = desc.clientName();
                    if (name == null || !name.startsWith(MCP_CLIENT_NAME_PREFIX)) {
                        continue;
                    }
                    UserPoolClientType full = cognitoClient.describeUserPoolClient(
                            DescribeUserPoolClientRequest.builder()
                                    .userPoolId(properties.userPoolId())
                                    .clientId(desc.clientId())
                                    .build()).userPoolClient();
                    List<String> callbacks = full.callbackURLs();
                    if (callbacks != null && new HashSet<>(callbacks).equals(requested)) {
                        return full;
                    }
                }
                nextToken = list.nextToken();
            } while (nextToken != null);
        } catch (CognitoIdentityProviderException e) {
            String detail = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
            LOGGER.warn("Idempotency lookup against Cognito failed; falling back to create. {}", detail);
        }
        return null;
    }

    private ClientRegistrationResponseDto toResponse(UserPoolClientType client, String clientName) {
        return new ClientRegistrationResponseDto(
                client.clientId(),
                client.clientSecret(),
                Instant.now().getEpochSecond(),
                0L,
                clientName,
                client.callbackURLs(),
                List.of("authorization_code", "refresh_token"),
                List.of("code"),
                "client_secret_basic",
                String.join(" ", properties.allowedScopes()));
    }

    private boolean isAllowedRedirectUri(String uri) {
        if (uri == null) {
            return false;
        }
        URI requested;
        try {
            requested = new URI(uri);
        } catch (URISyntaxException e) {
            return false;
        }
        return properties.allowedRedirectUris().stream().anyMatch(allowed -> matchesAllowed(requested, allowed));
    }

    /**
     * Matches a requested redirect URI against one allowlist entry. Scheme and host must
     * match exactly (case-insensitive). If the allowed entry specifies a port, the request
     * must match it; if not, any port is accepted (this is what makes {@code http://localhost}
     * a useful dev wildcard). The request path must either equal the allowed path or sit
     * under it as a sub-path, so {@code https://host/cb} does not match
     * {@code https://host/cballowed}.
     */
    private static boolean matchesAllowed(URI requested, String allowedString) {
        URI allowed;
        try {
            allowed = new URI(allowedString);
        } catch (URISyntaxException e) {
            return false;
        }
        if (requested.getScheme() == null || !requested.getScheme().equalsIgnoreCase(allowed.getScheme())) {
            return false;
        }
        if (requested.getHost() == null || !requested.getHost().equalsIgnoreCase(allowed.getHost())) {
            return false;
        }
        if (allowed.getPort() != -1 && allowed.getPort() != requested.getPort()) {
            return false;
        }
        String allowedPath = allowed.getPath() == null ? "" : allowed.getPath();
        String requestedPath = requested.getPath() == null ? "" : requested.getPath();
        if (allowedPath.isEmpty() || allowedPath.equals("/")) {
            return true;
        }
        return requestedPath.equals(allowedPath) || requestedPath.startsWith(allowedPath + "/");
    }

    private String buildCognitoClientName(String requestedName) {
        String base = (requestedName == null || requestedName.isBlank()) ? "mcp-client" : requestedName;
        String sanitized = base.replaceAll("[^a-zA-Z0-9-]", "-");
        String name = MCP_CLIENT_NAME_PREFIX + sanitized + "-" + Instant.now().toEpochMilli();
        return name.length() > COGNITO_CLIENT_NAME_MAX_LENGTH
                ? name.substring(0, COGNITO_CLIENT_NAME_MAX_LENGTH)
                : name;
    }
}
