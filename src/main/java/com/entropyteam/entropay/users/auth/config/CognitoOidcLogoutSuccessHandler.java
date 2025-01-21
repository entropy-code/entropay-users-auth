package com.entropyteam.entropay.users.auth.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CognitoOidcLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    @Value("${cognitoLogoutUrl}")
    private String cognitoLogoutUrl;
    @Value("${spring.security.oauth2.client.registration.cognito.clientId}")
    private String clientId;

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {

        return UriComponentsBuilder
                .fromUri(URI.create(cognitoLogoutUrl))
                .queryParam("client_id", clientId)
                .queryParam("logout_uri", request.getParameter("redirectUrl"))
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }
}
