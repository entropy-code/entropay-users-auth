package com.entropyteam.entropay.users.auth.controllers;

import static com.entropyteam.entropay.users.auth.config.AuthConstants.ROLE_ADMIN;
import static com.entropyteam.entropay.users.auth.config.AuthConstants.ROLE_ANALYST;
import static com.entropyteam.entropay.users.auth.config.AuthConstants.ROLE_DEVELOPMENT;
import static com.entropyteam.entropay.users.auth.config.AuthConstants.ROLE_MANAGER_HR;

import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;
import com.entropyteam.entropay.users.auth.config.TokenService;
import com.entropyteam.entropay.users.auth.dtos.UserDto;
import com.entropyteam.entropay.users.auth.services.UserService;

@RestController
@CrossOrigin
@RequestMapping(value = "/auth")
public class AuthController {

    private static final Logger LOGGER = LogManager.getLogger();

    private final UserService userService;
    private final TokenService tokenService;

    public AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(Authentication authentication, @RequestParam String redirectUrl) {
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        Map<String, Object> tokenClaims = oidcUser.getClaims();
        Optional<UserDto> authUser = userService.getUserByUsername(tokenService.getUsername(tokenClaims));
        if (authUser.isEmpty()) {
            throw new AuthException("User " + tokenService.getUsername(tokenClaims) + " not found");
        }

        tokenService.validateTokenRoles(tokenClaims, authUser.get().rolesByTenant());
        LOGGER.info("Login request for user: {}", tokenService.getUsername(tokenClaims));
        HttpHeaders headers = new HttpHeaders();
        headers.add("location", redirectUrl
                + "?token=" + oidcUser.getIdToken().getTokenValue()
                + "&expiresAt=" + oidcUser.getIdToken().getExpiresAt());
        return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);
    }

    @Secured({ROLE_ADMIN, ROLE_MANAGER_HR, ROLE_ANALYST, ROLE_DEVELOPMENT})
    @GetMapping("/identity")
    public ResponseEntity<UserDto> getIdentity(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        Map<String, Object> tokenClaims = jwt.getClaims();
        Optional<UserDto> authUser = userService.getUserByUsername(tokenService.getUsername(tokenClaims));
        if (authUser.isEmpty()) {
            throw new AuthException("User " + tokenService.getUsername(tokenClaims) + " not found");
        }

        LOGGER.info("Get identity request for user: {}", tokenService.getUsername(tokenClaims));
        return ResponseEntity.ok(authUser.get());
    }
}
