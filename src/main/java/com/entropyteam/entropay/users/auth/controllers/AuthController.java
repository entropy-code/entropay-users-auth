package com.entropyteam.entropay.users.auth.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;
import com.entropyteam.entropay.users.auth.dtos.UserDto;
import com.entropyteam.entropay.users.auth.services.UserService;

@RestController
@CrossOrigin
@RequestMapping(value = "/auth")
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public ResponseEntity<String> login(Authentication authentication, @RequestParam String redirectUrl) {
        LOGGER.info("Login request for user: {}", authentication);

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();

        HttpHeaders headers = new HttpHeaders();
        headers.add("location", redirectUrl
                + "?token=" + oidcUser.getIdToken().getTokenValue()
                + "&expiresAt=" + oidcUser.getIdToken().getExpiresAt());
        return new ResponseEntity<>(headers, HttpStatus.PERMANENT_REDIRECT);
    }

    @GetMapping("/identity")
    public ResponseEntity<UserDto> getIdentity(JwtAuthenticationToken authentication) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String email = (String) authentication.getToken().getClaims().get("email");
        UserDto authUser = userService.getUserByEmail(email)
                .orElseThrow(() -> new AuthException("User %s not found".formatted(email)));

        stopWatch.stop();
        LOGGER.info("Finish getting identity for user: {}, took: {}ms", email, stopWatch.getTotalTimeMillis());

        return ResponseEntity.ok(authUser);
    }
}
