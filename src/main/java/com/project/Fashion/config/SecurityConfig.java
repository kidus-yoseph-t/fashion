package com.project.Fashion.config;

import com.project.Fashion.security.JwtRequestFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler; // Import this

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Crucial for @PreAuthorize annotations
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8"); // Corrected charset
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\","
                    + "\"status\":401,"
                    + "\"error\":\"Unauthorized\","
                    + "\"message\":\"" + authException.getMessage() + "\","
                    + "\"path\":\"" + request.getRequestURI() + "\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .authorizeHttpRequests(authorize -> authorize
                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id:[0-9]+}", "/api/products/image/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers("/ws/**").permitAll() // WebSocket handshake
                        .requestMatchers("/api/contact").permitAll() // Contact form submission


                        // ===== USER MANAGEMENT (Self-service vs Admin) =====
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")

                        // ===== PRODUCT MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/{id:[0-9]+}").hasRole("SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/{id:[0-9]+}").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/products/{id:[0-9]+}/image").hasRole("SELLER")

                        // ===== CART MANAGEMENT (Typically Buyer) =====
                        .requestMatchers("/api/cart", "/api/cart/**").hasRole("BUYER")

                        // ===== ORDER MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/checkout").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/user/{userId}").hasAnyRole("ADMIN", "BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "BUYER", "SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/{id:[0-9]+}").hasRole("ADMIN")

                        // ===== REVIEW MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/reviews/product/{productId}/user/{userId}").hasRole("BUYER")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN")

                        // ===== CHAT MANAGEMENT =====
                        .requestMatchers("/api/chat/**").authenticated() // Any authenticated user can chat

                        // ===== Logout Endpoint Configuration (NEW) =====
                        .requestMatchers(HttpMethod.POST, "/api/users/logout").permitAll() // Explicitly permit POST for this endpoint


                        // ===== DEFAULT: ALL OTHER REQUESTS MUST BE AUTHENTICATED =====
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )
                // === ADD LOGOUT CONFIGURATION HERE ===
                .logout(logout -> logout
                                .logoutUrl("/api/users/logout") // The URL your frontend POSTs to for logout
                                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()) // Returns 200 OK on successful logout
                                .permitAll() // Allow everyone to access the logout endpoint
                        // No need for invalidateHttpSession or deleteCookies if purely stateless JWT is used
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}