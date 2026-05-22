package com.entropyteam.entropay.users.auth.dtos;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 Dynamic Client Registration request (RFC 7591). Unknown members are ignored
 * so clients that send extra metadata fields still register successfully.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClientRegistrationRequestDto(
        @JsonProperty("redirect_uris") List<String> redirectUris,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("grant_types") List<String> grantTypes,
        @JsonProperty("response_types") List<String> responseTypes,
        @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
        @JsonProperty("scope") String scope) {
}
