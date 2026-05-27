package com.entropyteam.entropay.users.auth.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 error response body (RFC 6749 / RFC 7591 §3.2.2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OAuthErrorDto(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription) {
}
