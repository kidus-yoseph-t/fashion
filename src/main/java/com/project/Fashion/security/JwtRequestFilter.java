package com.project.Fashion.security;

import com.project.Fashion.service.TokenBlocklistService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils; // For StringUtils.hasText

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlocklistService tokenBlocklistService; // Autowire TokenBlocklistService

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        String jti = null;

        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                // First, check if the token is blocklisted
                jti = jwtUtil.extractJti(jwt);
                if (jti != null && tokenBlocklistService.isTokenBlocked(jti)) {
                    logger.warn("Blocked JWT token received (JTI: {}). Access denied.", jti);
                    // Set response to unauthorized or let it fall through to be caught by security config
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\":\"Token is blocklisted. Please log in again.\"}");
                    response.setContentType("application/json");
                    return; // Stop further processing for blocklisted tokens
                }
                // If not blocklisted, proceed to extract username
                username = jwtUtil.extractUsername(jwt);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token (IllegalArgumentException): {}", e.getMessage());
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired: {}", e.getMessage());
            } catch (SignatureException e) { // This is io.jsonwebtoken.SignatureException
                logger.error("JWT signature does not match locally computed signature: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                logger.error("Invalid JWT token (MalformedJwtException): {}", e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.error("JWT token is unsupported: {}", e.getMessage());
            } catch (Exception e) { // Catch-all for any other JWT parsing errors
                logger.error("Generic error parsing JWT token: {}", e.getMessage());
            }
        } else {
            // logger.debug("JWT Token does not begin with Bearer String or is null for request: {}", request.getRequestURI());
        }


        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // We've already checked for blocklist. If username is not null, it means token was not blocklisted and potentially valid.
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) { // validateToken also checks expiry
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                logger.debug("Authenticated user {} with roles {} for request: {}", username, userDetails.getAuthorities(), request.getRequestURI());
            } else {
                logger.warn("JWT token validation failed for user {}. JWT: {}", username, jwt);
            }
        }
        chain.doFilter(request, response);
    }
}
