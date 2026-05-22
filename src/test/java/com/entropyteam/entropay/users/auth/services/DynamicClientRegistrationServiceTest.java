package com.entropyteam.entropay.users.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.entropyteam.entropay.users.auth.common.exceptions.ClientRegistrationException;
import com.entropyteam.entropay.users.auth.config.McpAuthGatewayProperties;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationRequestDto;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationResponseDto;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;

@ExtendWith(MockitoExtension.class)
class DynamicClientRegistrationServiceTest {

    @Mock
    private CognitoIdentityProviderClient cognitoClient;

    private DynamicClientRegistrationService service;

    @BeforeEach
    void setUp() {
        McpAuthGatewayProperties properties = new McpAuthGatewayProperties(
                "http://localhost:8000",
                "https://entropay-dev.auth.us-east-1.amazoncognito.com",
                "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_HqQSJJ2NG",
                "us-east-1_HqQSJJ2NG",
                List.of("openid", "email", "profile"),
                List.of("https://claude.ai/api/mcp/auth_callback", "http://localhost"),
                List.of("COGNITO", "Google"));
        service = new DynamicClientRegistrationService(cognitoClient, properties);
    }

    @Test
    void shouldRegisterClientForAllowedRedirectUri() {
        // Given
        UserPoolClientType created = UserPoolClientType.builder()
                .clientId("generated-client-id")
                .clientSecret("generated-secret")
                .callbackURLs("https://claude.ai/api/mcp/auth_callback")
                .build();
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
                .thenReturn(CreateUserPoolClientResponse.builder().userPoolClient(created).build());
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of("https://claude.ai/api/mcp/auth_callback"), "Claude", null, null, null, null);

        // When
        ClientRegistrationResponseDto response = service.register(request);

        // Then
        assertEquals("generated-client-id", response.clientId());
        assertEquals("generated-secret", response.clientSecret());
        assertEquals(0L, response.clientSecretExpiresAt());
    }

    @Test
    void shouldRejectRedirectUriNotInAllowlist() {
        // Given
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of("https://evil.example.com/callback"), "Attacker", null, null, null, null);

        // When / Then
        ClientRegistrationException exception = assertThrows(ClientRegistrationException.class,
                () -> service.register(request));
        assertEquals("invalid_redirect_uri", exception.getError());
    }

    @Test
    void shouldRejectMissingRedirectUris() {
        // Given
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of(), "NoUris", null, null, null, null);

        // When / Then
        ClientRegistrationException exception = assertThrows(ClientRegistrationException.class,
                () -> service.register(request));
        assertEquals("invalid_client_metadata", exception.getError());
    }

    @Test
    void shouldMapCognitoFailureToClientRegistrationException() {
        // Given
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
                .thenThrow(CognitoIdentityProviderException.builder().message("boom").build());
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of("https://claude.ai/api/mcp/auth_callback"), "Claude", null, null, null, null);

        // When / Then
        ClientRegistrationException exception = assertThrows(ClientRegistrationException.class,
                () -> service.register(request));
        assertEquals("invalid_client_metadata", exception.getError());
    }
}
