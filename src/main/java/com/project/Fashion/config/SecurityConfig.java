package com.project.Fashion.config;

import com.project.Fashion.security.CustomLogoutHandler;
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
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler; // Can be removed or kept if a specific status is desired post-blocklisting.

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Autowired
    private CustomLogoutHandler customLogoutHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"timestamp\":\"" + java.time.LocalDateTime.now() + "\","
                    + "\"status\":401,"
                    + "\"error\":\"Unauthorized\","
                    + "\"message\":\"" + authException.getMessage() + "\"," // Keep original message for context
                    + "\"path\":\"" + request.getRequestURI() + "\"}");
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {}) // Assuming CorsConfig is handling detailed CORS settings
                .authorizeHttpRequests(authorize -> authorize
                        // SWAGGER
                        .requestMatchers(
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // PUBLIC ENDPOINTS
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id:[0-9]+}", "/api/products/image/**", "/api/products/categories", "/api/products/price-range-meta").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/product/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/deliveries", "/api/deliveries/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/contact").permitAll()
                        // USER MANAGEMENT
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated() // Any authenticated user
                        .requestMatchers(HttpMethod.PATCH, "/api/users/me").authenticated() // Any authenticated user
                        .requestMatchers(HttpMethod.POST, "/api/users/admin/create-user").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/users/{id}").hasAnyRole("ADMIN") // Or specific checks like @userSecurity.isOwner
                        .requestMatchers(HttpMethod.PUT, "/api/users/{id}").hasAnyRole("ADMIN") // Or @userSecurity.isOwner
                        .requestMatchers(HttpMethod.PATCH, "/api/users/{id}").hasAnyRole("ADMIN") // Or @userSecurity.isOwner
                        .requestMatchers(HttpMethod.DELETE, "/api/users/{id}").hasRole("ADMIN")
                        // PRODUCT MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                        .requestMatchers(HttpMethod.GET, "/api/products/seller/me").hasRole("SELLER") // Seller's own products
                        .requestMatchers(HttpMethod.PUT, "/api/products/{id:[0-9]+}").hasRole("SELLER") // Ownership checked by @productSecurity
                        .requestMatchers(HttpMethod.DELETE, "/api/products/{id:[0-9]+}").hasAnyRole("SELLER", "ADMIN") // Ownership checked by @productSecurity for SELLER
                        .requestMatchers(HttpMethod.POST, "/api/products/{id:[0-9]+}/image").hasRole("SELLER") // Ownership checked
                        // CART MANAGEMENT
                        .requestMatchers("/api/cart", "/api/cart/**").hasRole("BUYER")
                        // FAVORITES MANAGEMENT
                        .requestMatchers("/api/users/me/favorites", "/api/users/me/favorites/**").hasRole("BUYER")
                        // ORDER MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/checkout").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/user/{userId}").hasAnyRole("ADMIN", "BUYER") // Further checks in controller/service
                        .requestMatchers(HttpMethod.GET, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "BUYER", "SELLER") // Further checks in controller/service
                        .requestMatchers(HttpMethod.PUT, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER") // Further checks in controller/service
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/{id:[0-9]+}").hasAnyRole("ADMIN", "SELLER") // Further checks in controller/service
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/{id:[0-9]+}").hasRole("ADMIN")
                        // REVIEW MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/reviews/product/{productId}/user/{userId}").hasRole("BUYER") // Further purchase check in service
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN") // Ownership check in controller
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/{reviewId}").hasAnyRole("BUYER", "ADMIN") // Ownership check in controller
                        // PAYMENT MANAGEMENT
                        .requestMatchers(HttpMethod.POST, "/api/payments/process").hasAnyRole("BUYER", "ADMIN") // Further checks in controller
                        // CHAT MANAGEMENT
                        .requestMatchers("/api/chat/**").authenticated()
                        // ADMIN MESSAGES (Contact form submissions)
                        .requestMatchers("/api/AdminMessages", "/api/AdminMessages/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/settings").hasRole("ADMIN")
                        // LOGOUT
                        .requestMatchers(HttpMethod.POST, "/api/users/logout").authenticated() // Any authenticated user can log out
                        // DEFAULT RULE
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )
                .logout(logout -> logout
                        .logoutUrl("/api/users/logout") // The endpoint for logout
                        .addLogoutHandler(customLogoutHandler) // Add our custom handler
                        // No need for .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler()) for stateless
                        // The customLogoutHandler handles the logic, and the response can be 200 OK by default or customized.
                        // If a specific success response is needed, the CustomLogoutHandler can write to the response.
                        // For now, let's assume default behavior (200 OK if no error in handler) is fine.
                        .invalidateHttpSession(false) // No HTTP session to invalidate
                        .clearAuthentication(true)    // Clear SecurityContextHolder
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
