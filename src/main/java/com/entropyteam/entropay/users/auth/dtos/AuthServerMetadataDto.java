package com.entropyteam.entropay.users.auth.dtos;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 Authorization Server Metadata (RFC 8414). Served by the MCP auth gateway so
 * that MCP clients can discover the authorize/token endpoints (delegated to AWS Cognito)
 * and the Dynamic Client Registration endpoint (this service).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthServerMetadataDto(
        @JsonProperty("issuer") String issuer,
        @JsonProperty("authorization_endpoint") String authorizationEndpoint,
        @JsonProperty("token_endpoint") String tokenEndpoint,
        @JsonProperty("registration_endpoint") String registrationEndpoint,
        @JsonProperty("jwks_uri") String jwksUri,
        @JsonProperty("userinfo_endpoint") String userinfoEndpoint,
        @JsonProperty("scopes_supported") List<String> scopesSupported,
        @JsonProperty("response_types_supported") List<String> responseTypesSupported,
        @JsonProperty("grant_types_supported") List<String> grantTypesSupported,
        @JsonProperty("token_endpoint_auth_methods_supported") List<String> tokenEndpointAuthMethodsSupported,
        @JsonProperty("code_challenge_methods_supported") List<String> codeChallengeMethodsSupported) {
}
