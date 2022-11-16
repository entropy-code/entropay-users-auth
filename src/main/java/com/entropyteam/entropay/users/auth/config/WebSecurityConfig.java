package com.entropyteam.entropay.users.auth.config;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig {

    public static final String ACTUATOR_URL = "/actuator/**";
    public static final String LOGIN_URL = "/auth/login";
    public static final String LOGOUT_URL = "/auth/logout";
    public static final String COOKIE_NAME = "JSESSIONID";

    private final CognitoOidcLogoutSuccessHandler cognitoOidcLogoutSuccessHandler;
    private final TokenService tokenService;

    public WebSecurityConfig(CognitoOidcLogoutSuccessHandler cognitoOidcLogoutSuccessHandler,
            TokenService tokenService) {
        this.cognitoOidcLogoutSuccessHandler = cognitoOidcLogoutSuccessHandler;
        this.tokenService = tokenService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.cors().and().csrf().disable()
                // Allow actuator
                .authorizeRequests().antMatchers(ACTUATOR_URL).permitAll()

                // Use oauth2Login for login url
                .antMatchers(LOGIN_URL).authenticated().and()
                .oauth2Login()
                .userInfoEndpoint()
                .and().and()

                // Logout Url
                .logout().logoutUrl(LOGOUT_URL)
                .logoutSuccessHandler(cognitoOidcLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies(COOKIE_NAME).and()

                // Use Jwt for the rest of the api
                .authorizeRequests().anyRequest().authenticated().and()
                .oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtCustomConverter());
        return httpSecurity.build();
    }

    @Bean
    public Converter<Jwt, JwtAuthenticationToken> jwtCustomConverter() {
        return jwt -> new JwtAuthenticationToken(jwt, getGrantedAuthorities(jwt.getClaims()));
    }

    private Set<GrantedAuthority> getGrantedAuthorities(Map<String, Object> appClaims) {
        Map<String, Collection<String>> rolesByTenant = tokenService.getRoles(appClaims);
        //TODO get role for current tenant
        Collection<String> roles = rolesByTenant.values().stream().findFirst().orElse(Collections.emptySet());
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @Bean
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
        final FilterRegistrationBean<ForwardedHeaderFilter> filterRegistrationBean =
                new FilterRegistrationBean<ForwardedHeaderFilter>();
        filterRegistrationBean.setFilter(new ForwardedHeaderFilter());
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filterRegistrationBean;
    }
}
