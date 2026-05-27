package com.entropyteam.entropay.users.auth.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.entropyteam.entropay.users.auth.config.McpAuthGatewayProperties;
import com.entropyteam.entropay.users.auth.dtos.AuthServerMetadataDto;

class OAuthMetadataControllerTest {

    @Test
    void shouldExposeMetadataWithIssuerEqualToPublicBaseUrl() {
        // Given — the RFC 8414 issuer MUST match the URL the document is served from
        McpAuthGatewayProperties properties = new McpAuthGatewayProperties(
                "http://localhost:8000",
                "https://entropay-dev.auth.us-east-1.amazoncognito.com",
                "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_HqQSJJ2NG",
                "us-east-1_HqQSJJ2NG",
                List.of("openid", "email", "profile"),
                List.of("https://claude.ai/api/mcp/auth_callback"),
                List.of("COGNITO", "Google"));
        OAuthMetadataController controller = new OAuthMetadataController(properties);

        // When
        AuthServerMetadataDto metadata = controller.authorizationServerMetadata().getBody();

        // Then
        assertEquals("http://localhost:8000", metadata.issuer());
        assertEquals("http://localhost:8000/oauth2/register", metadata.registrationEndpoint());
        assertEquals("https://entropay-dev.auth.us-east-1.amazoncognito.com/oauth2/authorize",
                metadata.authorizationEndpoint());
        assertEquals("https://entropay-dev.auth.us-east-1.amazoncognito.com/oauth2/token",
                metadata.tokenEndpoint());
        assertTrue(metadata.codeChallengeMethodsSupported().contains("S256"));
    }
}
