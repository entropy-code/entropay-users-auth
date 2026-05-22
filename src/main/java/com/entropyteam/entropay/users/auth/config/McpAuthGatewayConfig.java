package com.entropyteam.entropay.users.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring for the MCP OAuth2 auth gateway: enables {@link McpAuthGatewayProperties} and
 * registers the rate-limit filter scoped to the Dynamic Client Registration endpoint.
 */
@Configuration
@EnableConfigurationProperties(McpAuthGatewayProperties.class)
public class McpAuthGatewayConfig {

    @Bean
    public FilterRegistrationBean<DcrRateLimitFilter> dcrRateLimitFilter() {
        FilterRegistrationBean<DcrRateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new DcrRateLimitFilter());
        registration.addUrlPatterns("/oauth2/register");
        registration.setName("dcrRateLimitFilter");
        return registration;
    }
}
