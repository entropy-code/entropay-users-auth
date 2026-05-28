package com.entropyteam.entropay.users.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

/**
 * Wiring for the MCP OAuth2 auth gateway: enables {@link McpAuthGatewayProperties},
 * registers the rate-limit filter scoped to the Dynamic Client Registration endpoint,
 * and provides the {@link CognitoIdentityProviderClient} the DCR service uses to create
 * Cognito app clients on demand.
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

    @Bean
    public CognitoIdentityProviderClient cognitoIdentityProviderClient(McpAuthGatewayProperties properties) {
        String userPoolId = properties.userPoolId();
        int sep = userPoolId.indexOf('_');
        if (sep <= 0) {
            throw new IllegalStateException(
                    "mcp.auth-gateway.user-pool-id must be of the form '<region>_<id>', was: " + userPoolId);
        }
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(userPoolId.substring(0, sep)))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
