package com.entropyteam.entropay.users.auth.dtos;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 Dynamic Client Registration response (RFC 7591). {@code clientSecretExpiresAt}
 * is 0 to indicate the secret does not expire.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientRegistrationResponseDto(
        @JsonProperty("client_id") String clientId,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("client_id_issued_at") Long clientIdIssuedAt,
        @JsonProperty("client_secret_expires_at") Long clientSecretExpiresAt,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("redirect_uris") List<String> redirectUris,
        @JsonProperty("grant_types") List<String> grantTypes,
        @JsonProperty("response_types") List<String> responseTypes,
        @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
        @JsonProperty("scope") String scope) {
}
