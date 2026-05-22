package com.entropyteam.entropay.users.auth.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
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
        // Given — no pre-existing mcp-* client to reuse
        when(cognitoClient.listUserPoolClients(any(ListUserPoolClientsRequest.class)))
                .thenReturn(emptyClientList());
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
    void shouldReuseExistingMcpClientWithMatchingCallbacks() {
        // Given — an mcp-* client already exists with the exact same callback URL
        UserPoolClientDescription existingDesc = UserPoolClientDescription.builder()
                .clientId("existing-client-id")
                .clientName("mcp-claude-1779000000000")
                .build();
        UserPoolClientType existingFull = UserPoolClientType.builder()
                .clientId("existing-client-id")
                .clientSecret("existing-secret")
                .clientName("mcp-claude-1779000000000")
                .callbackURLs("https://claude.ai/api/mcp/auth_callback")
                .build();
        when(cognitoClient.listUserPoolClients(any(ListUserPoolClientsRequest.class)))
                .thenReturn(ListUserPoolClientsResponse.builder()
                        .userPoolClients(existingDesc).build());
        when(cognitoClient.describeUserPoolClient(any(DescribeUserPoolClientRequest.class)))
                .thenReturn(DescribeUserPoolClientResponse.builder()
                        .userPoolClient(existingFull).build());
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of("https://claude.ai/api/mcp/auth_callback"), "Claude", null, null, null, null);

        // When
        ClientRegistrationResponseDto response = service.register(request);

        // Then — the existing client is returned and no new one is created
        assertEquals("existing-client-id", response.clientId());
        assertEquals("existing-secret", response.clientSecret());
        verify(cognitoClient, never()).createUserPoolClient(any(CreateUserPoolClientRequest.class));
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
        // Given — no pre-existing match, and createUserPoolClient blows up
        when(cognitoClient.listUserPoolClients(any(ListUserPoolClientsRequest.class)))
                .thenReturn(emptyClientList());
        when(cognitoClient.createUserPoolClient(any(CreateUserPoolClientRequest.class)))
                .thenThrow(CognitoIdentityProviderException.builder().message("boom").build());
        ClientRegistrationRequestDto request = new ClientRegistrationRequestDto(
                List.of("https://claude.ai/api/mcp/auth_callback"), "Claude", null, null, null, null);

        // When / Then
        ClientRegistrationException exception = assertThrows(ClientRegistrationException.class,
                () -> service.register(request));
        assertEquals("invalid_client_metadata", exception.getError());
    }

    private static ListUserPoolClientsResponse emptyClientList() {
        return ListUserPoolClientsResponse.builder()
                .userPoolClients(List.<UserPoolClientDescription>of())
                .build();
    }
}
