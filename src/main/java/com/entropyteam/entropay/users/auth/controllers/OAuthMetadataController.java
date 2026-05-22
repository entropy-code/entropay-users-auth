package com.entropyteam.entropay.users.auth.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.config.McpAuthGatewayProperties;
import com.entropyteam.entropay.users.auth.dtos.AuthServerMetadataDto;

/**
 * Publishes OAuth 2.0 Authorization Server Metadata (RFC 8414) for the MCP auth gateway.
 * The same document is served at the OpenID Connect Discovery path so MCP clients that
 * probe either well-known URL succeed. The authorize/token endpoints delegate to AWS
 * Cognito; the registration endpoint is this service's Dynamic Client Registration proxy.
 */
@RestController
@CrossOrigin
public class OAuthMetadataController {

    private final McpAuthGatewayProperties properties;

    public OAuthMetadataController(McpAuthGatewayProperties properties) {
        this.properties = properties;
    }

    @GetMapping({"/.well-known/oauth-authorization-server", "/.well-known/openid-configuration"})
    public ResponseEntity<AuthServerMetadataDto> authorizationServerMetadata() {
        String base = properties.publicBaseUrl();
        String cognitoDomain = properties.cognitoDomain();
        AuthServerMetadataDto metadata = new AuthServerMetadataDto(
                base,
                cognitoDomain + "/oauth2/authorize",
                cognitoDomain + "/oauth2/token",
                base + "/oauth2/register",
                properties.cognitoIssuer() + "/.well-known/jwks.json",
                cognitoDomain + "/oauth2/userInfo",
                properties.allowedScopes(),
                List.of("code"),
                List.of("authorization_code", "refresh_token"),
                List.of("client_secret_basic", "client_secret_post"),
                List.of("S256"));
        return ResponseEntity.ok(metadata);
    }
}
