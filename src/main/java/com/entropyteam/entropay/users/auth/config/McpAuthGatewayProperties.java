package com.entropyteam.entropay.users.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the MCP OAuth2 auth gateway: the synthesized Authorization Server
 * metadata (RFC 8414) and the Dynamic Client Registration (RFC 7591) endpoint that this
 * service exposes in front of AWS Cognito so that MCP clients can self-register.
 */
@ConfigurationProperties(prefix = "mcp.auth-gateway")
public record McpAuthGatewayProperties(
        /** Public base URL this service is reachable at; becomes the metadata {@code issuer}. */
        String publicBaseUrl,
        /** Cognito hosted-UI domain, e.g. https://entropay-dev.auth.us-east-1.amazoncognito.com */
        String cognitoDomain,
        /** Cognito issuer URL, e.g. https://cognito-idp.us-east-1.amazonaws.com/us-east-1_HqQSJJ2NG */
        String cognitoIssuer,
        /** Cognito user pool id new app clients are created in. */
        String userPoolId,
        /** OAuth2 scopes advertised in metadata and granted to registered clients. */
        List<String> allowedScopes,
        /** redirect_uri prefixes a registration request is allowed to use. */
        List<String> allowedRedirectUris,
        /** Cognito identity providers enabled on registered app clients (e.g. COGNITO, Google). */
        List<String> identityProviders) {
}
