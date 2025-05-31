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
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Crucial for @PreAuthorize annotations
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    // UserDetailsServiceImpl and PasswordEncoder are expected to be available as beans.
    // Spring Boot autoconfigures DaoAuthenticationProvider if UserDetailsService and PasswordEncoder beans are present.

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
                .cors(cors -> {}) // Assuming you have a CorsFilter bean defined elsewhere (like CorsConfig.java)
                .authorizeHttpRequests(authorize -> authorize
                        // ===== PUBLIC ENDPOINTS =====
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id:[0-9]+}", "/api/products/image/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers("/ws/**").permitAll() // WebSocket handshake

                        // ===== USER MANAGEMENT (Self-service vs Admin) =====
                        // Specific user actions (like GET self, PUT self) are often handled by @PreAuthorize("... or @userSecurity.isOwner(...)")
                        // For general admin access to user management:
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER") // Further checks with @PreAuthorize
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER")   // Further checks with @PreAuthorize
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{id}").hasAnyRole("ADMIN", "BUYER", "SELLER") // Further checks with @PreAuthorize
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")

                        // ===== PRODUCT MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/{id:[0-9]+}").hasRole("SELLER") // Seller can update (their own - checked via @PreAuthorize)
                        .requestMatchers(HttpMethod.DELETE, "/api/products/{id:[0-9]+}").hasAnyRole("SELLER", "ADMIN") // Seller (own) or Admin (any)
                        .requestMatchers(HttpMethod.POST, "/api/products/{id:[0-9]+}/image").hasRole("SELLER")

                        // ===== CART MANAGEMENT (Typically Buyer) =====
                        .requestMatchers("/api/cart", "/api/cart/**").hasRole("BUYER")

                        // ===== ORDER MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/checkout").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN") // Admin gets all orders
                        .requestMatchers(HttpMethod.GET, "/api/orders/user/{userId}").hasAnyRole("ADMIN", "BUYER") // Buyer for own, Admin for any. Checked by @PreAuthorize
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "BUYER", "SELLER") // All can potentially view an order, details checked by @PreAuthorize
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER") // Admin or Seller (for their product order status)
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/{id:[0-9]+}").hasRole("ADMIN")

                        // ===== REVIEW MANAGEMENT =====
                        .requestMatchers(HttpMethod.POST, "/api/reviews/product/{productId}/user/{userId}").hasRole("BUYER") // Buyer posts reviews
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN") // Buyer (own) or Admin
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN") // Buyer (own) or Admin

                        // ===== CHAT MANAGEMENT =====
                        .requestMatchers("/api/chat/**").authenticated() // Any authenticated user can chat

                        // ===== DEFAULT: ALL OTHER REQUESTS MUST BE AUTHENTICATED =====
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}