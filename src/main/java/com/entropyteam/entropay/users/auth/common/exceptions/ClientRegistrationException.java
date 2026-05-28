package com.entropyteam.entropay.users.auth.common.exceptions;

/**
 * Raised when an OAuth 2.0 Dynamic Client Registration (RFC 7591) request is rejected.
 * Carries the RFC 7591 error code (e.g. {@code invalid_redirect_uri},
 * {@code invalid_client_metadata}) so the controller can return a spec-compliant body.
 */
public class ClientRegistrationException extends RuntimeException {

    private final String error;

    public ClientRegistrationException(String error, String description) {
        super(description);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
