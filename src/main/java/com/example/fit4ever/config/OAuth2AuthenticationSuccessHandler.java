package com.example.fit4ever.config;

import com.example.fit4ever.service.OAuth2UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to target URL");
            return;
        }

        OAuth2UserPrincipal userPrincipal = (OAuth2UserPrincipal) authentication.getPrincipal();
        String email = userPrincipal.getUser().getEmail();
        
        // Generate JWT token
        String token = jwtUtil.generateToken(email);
        
        log.info("OAuth2 authentication successful for user: {}", email);
        
        // Always redirect to the same domain
        String targetUrl = UriComponentsBuilder.fromUriString("/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
    
    private boolean isDevelopmentEnvironment(HttpServletRequest request) {
        String serverName = request.getServerName();
        return "localhost".equals(serverName) || "127.0.0.1".equals(serverName);
    }
}
