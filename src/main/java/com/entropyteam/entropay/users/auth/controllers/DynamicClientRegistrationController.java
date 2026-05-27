package com.entropyteam.entropay.users.auth.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.common.exceptions.ClientRegistrationException;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationRequestDto;
import com.entropyteam.entropay.users.auth.dtos.ClientRegistrationResponseDto;
import com.entropyteam.entropay.users.auth.dtos.OAuthErrorDto;
import com.entropyteam.entropay.users.auth.services.DynamicClientRegistrationService;

/**
 * OAuth 2.0 Dynamic Client Registration endpoint (RFC 7591). MCP clients POST their
 * metadata here to obtain a Cognito-backed client_id / client_secret. Public by design:
 * registration happens before any token exists. Abuse is constrained by the redirect_uri
 * allowlist (see {@link DynamicClientRegistrationService}) and a per-IP rate limit.
 */
@RestController
@CrossOrigin
public class DynamicClientRegistrationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicClientRegistrationController.class);

    private final DynamicClientRegistrationService registrationService;

    public DynamicClientRegistrationController(DynamicClientRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/oauth2/register")
    public ResponseEntity<?> register(@RequestBody ClientRegistrationRequestDto request) {
        try {
            ClientRegistrationResponseDto response = registrationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ClientRegistrationException e) {
            LOGGER.warn("Client registration rejected ({}): {}", e.getError(), e.getMessage());
            return ResponseEntity.badRequest().body(new OAuthErrorDto(e.getError(), e.getMessage()));
        }
    }
}
