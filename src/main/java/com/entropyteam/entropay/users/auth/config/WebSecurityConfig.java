package com.entropyteam.entropay.users.auth.config;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.ForwardedHeaderFilter;
import com.entropyteam.entropay.users.auth.common.exceptions.AuthException;
import com.entropyteam.entropay.users.auth.dtos.UserDto;
import com.entropyteam.entropay.users.auth.services.UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

    public static final String ACTUATOR_URL = "/actuator/**";
    public static final String LOGIN_URL = "/auth/login";
    public static final String LOGOUT_URL = "/auth/logout";
    public static final String COOKIE_NAME = "JSESSIONID";

    private final CognitoOidcLogoutSuccessHandler cognitoOidcLogoutSuccessHandler;
    private final UserService userService;

    public WebSecurityConfig(CognitoOidcLogoutSuccessHandler cognitoOidcLogoutSuccessHandler,
            UserService userService) {
        this.cognitoOidcLogoutSuccessHandler = cognitoOidcLogoutSuccessHandler;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ACTUATOR_URL).permitAll()
                        .requestMatchers(LOGIN_URL).authenticated()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())

                // Logout Url
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler(cognitoOidcLogoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies(COOKIE_NAME))

                // Use Jwt for the rest of the api
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtCustomConverter())));

        return httpSecurity.build();
    }

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> jwtCustomConverter() {
        return jwt -> {
            String email = (String) jwt.getClaims().get("email");
            UserDto authUser = userService.getUserByEmail(email)
                    .orElseThrow(() -> new AuthException("User %s not found".formatted(email)));
            Set<GrantedAuthority> authorities = getGrantedAuthorities(authUser.rolesByTenant());
            return new JwtAuthenticationToken(jwt, authorities);
        };
    }

    private Set<GrantedAuthority> getGrantedAuthorities(Map<UUID, Set<String>> rolesByTenant) {
        //TODO get role for current tenant
        Collection<String> roles = rolesByTenant.values().stream().findFirst().orElse(Collections.emptySet());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Bean
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        final FilterRegistrationBean<ForwardedHeaderFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new ForwardedHeaderFilter());
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }
}
