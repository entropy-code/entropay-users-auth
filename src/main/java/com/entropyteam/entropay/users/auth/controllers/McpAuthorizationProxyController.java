package com.entropyteam.entropay.users.auth.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.entropyteam.entropay.users.auth.config.McpAuthGatewayProperties;

/**
 * Front-end for Cognito's {@code /oauth2/authorize} that strips the RFC 8707
 * {@code resource} parameter before redirecting. Cognito user pools in the ESSENTIALS
 * tier with Managed Login v2 accept {@code resource} on /authorize but the resulting
 * authorization code then fails the /token exchange with {@code invalid_grant}. MCP
 * clients (Claude custom connectors) follow MCP spec and always send {@code resource}
 * for audience binding, so the only way to make the flow work end-to-end is to drop
 * the parameter at the edge.
 *
 * <p>The {@code authorization_endpoint} advertised in our RFC 8414 metadata points at
 * this controller instead of Cognito's domain directly, so the MCP client redirects here
 * first and from here is forwarded to Cognito with a clean query string.
 */
@RestController
public class McpAuthorizationProxyController {

    private final McpAuthGatewayProperties properties;

    public McpAuthorizationProxyController(McpAuthGatewayProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/mcp/oauth2/authorize")
    public ResponseEntity<Void> proxyAuthorize(HttpServletRequest request) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if ("resource".equals(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (!query.isEmpty()) {
                    query.append('&');
                }
                query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        String location = properties.cognitoDomain() + "/oauth2/authorize?" + query;
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, location).build();
    }
}
