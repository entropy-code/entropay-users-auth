package com.entropyteam.entropay.users.auth.controllers;

import static com.entropyteam.entropay.users.auth.config.AuthConstants.ROLE_ADMIN;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;
import com.entropyteam.entropay.users.auth.config.TokenUtils;
import com.entropyteam.entropay.users.auth.dtos.UserDto;
import com.entropyteam.entropay.users.auth.services.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
@RequestMapping(value = "/auth")
public class AuthController {

    private final UserService userService;

    @Value("${appHomeUrl}")
    private String loginSuccessRedirectUrl;

    @GetMapping("/login")
    public ResponseEntity<String> login(Authentication authentication) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        Map<String, Object> tokenClaims = oidcUser.getClaims();
        Optional<UserDto> authUser = userService.getUserByUsername(TokenUtils.getUsername(tokenClaims));
        if (authUser.isEmpty()) {
            throw new AuthException("User " + TokenUtils.getUsername(tokenClaims) + " not found");
        }

        TokenUtils.validateTokenRoles(tokenClaims, authUser.get().rolesByTenant());
        log.info("Login request for user: {}", TokenUtils.getUsername(tokenClaims));
        HttpHeaders headers = new HttpHeaders();
        headers.add("location", loginSuccessRedirectUrl
                + "?token=" + oidcUser.getIdToken().getTokenValue()
                + "&expiresAt=" + oidcUser.getIdToken().getExpiresAt());
        return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);
    }

    @Secured(ROLE_ADMIN)
    @GetMapping("/identity")
    public ResponseEntity<UserDto> getIdentity(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Map<String, Object> tokenClaims = jwt.getClaims();
        Optional<UserDto> authUser = userService.getUserByUsername(TokenUtils.getUsername(tokenClaims));
        if (authUser.isEmpty()) {
            throw new AuthException("User " + TokenUtils.getUsername(tokenClaims) + " not found");
        }

        log.info("Get identity request for user: {}", TokenUtils.getUsername(tokenClaims));
         return ResponseEntity.ok(authUser.get());
    }
}
