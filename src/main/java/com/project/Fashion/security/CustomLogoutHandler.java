package com.project.Fashion.security;

import com.project.Fashion.service.TokenBlocklistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Component
public class CustomLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomLogoutHandler.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlocklistService tokenBlocklistService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        final String authorizationHeader = request.getHeader("Authorization");
        String jwt = null;
        String jti = null;
        Date expirationDate = null;

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                jti = jwtUtil.extractJti(jwt);
                expirationDate = jwtUtil.extractExpiration(jwt);

                if (jti != null && expirationDate != null) {
                    tokenBlocklistService.blockToken(jti, expirationDate);
                    logger.info("User logged out. Token JTI {} has been blocklisted.", jti);
                } else {
                    logger.warn("Could not block token on logout: JTI or expiration was null. JWT: {}", jwt);
                }
            } catch (Exception e) {
                logger.error("Error processing token for blocklisting during logout: {}", e.getMessage());
                // Don't let an error here prevent logout flow from completing,
                // but log it as it indicates an issue (e.g., malformed token).
            }
        } else {
            logger.warn("Logout attempt without Bearer token in Authorization header.");
        }

        // Note: For a stateless API, clearing SecurityContextHolder is typically done by Spring Security
        // after the filter chain, or it's implicitly cleared on next request if token is invalid.
        // Explicit clearing here is generally not harmful but also not strictly necessary
        // if the main goal is just to blocklist the token.
        // SecurityContextHolder.clearContext(); // Optional: Spring Security usually handles this.
    }
}
