package com.project.Fashion.config;//package com.project.Fashion.config;

//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // For @PreAuthorize
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity // Enables Spring Security's web security features
//@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize, @PostAuthorize etc.
//public class SecurityConfig {
//
//    private final UserDetailsService userDetailsService; // Your UserDetailsServiceImpl
//
//    // Inject UserDetailsService (e.g., from your security/UserDetailsServiceImpl.java)
//    public SecurityConfig(UserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(csrf -> csrf.disable()) // Disable CSRF for API-only if you don't handle it. For web apps, you should enable and handle it.
//                .authorizeHttpRequests(authorize -> authorize
//                        // Public endpoints - accessible without authentication
//                        .requestMatchers("/login", "/register", "/api/auth/**").permitAll()
//                        // Specific role-based access for authenticated users
//                        .requestMatchers("/api/products/**", "/api/categories/**").hasAnyRole("BUYER", "SELLER")
//                        .requestMatchers("/api/seller/dashboard/**", "/api/seller/products/**").hasRole("SELLER")
//                        // All other requests require authentication
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/login") // Specify your custom login page URL (if you have one)
//                        .loginProcessingUrl("/perform_login") // The URL to which the login form data is submitted
//                        .defaultSuccessUrl("/home", true) // Redirect after successful login (true forces redirection even if user tries to access a different page)
//                        .failureUrl("/login?error") // Redirect after failed login
//                        .permitAll() // Allow everyone to access the login page and process URL
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout") // The URL to trigger logout
//                        .logoutSuccessUrl("/login?logout") // Redirect after successful logout
//                        .invalidateHttpSession(true) // Invalidate the HTTP session
//                        .deleteCookies("JSESSIONID") // Delete the session cookie
//                        .permitAll() // Allow everyone to access the logout URL
//                )
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Spring will create a session if one doesn't exist
//                        .maximumSessions(1) // Limit concurrent sessions for a user
//                        .maxSessionsPreventsLogin(true) // If a new login attempt occurs when max sessions are reached, prevent the new login
//                );
//
//        return http.build();
//    }
//
//    // Configure the AuthenticationProvider to use your UserDetailsService and PasswordEncoder
//    @Bean
//    public DaoAuthenticationProvider daoAuthenticationProvider() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//        provider.setUserDetailsService(userDetailsService); // Inject your UserDetailsService
//        provider.setPasswordEncoder(passwordEncoder()); // Use the PasswordEncoder bean
//        return provider;
//    }
//
//    // Define your PasswordEncoder bean
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(); // Or Argon2, SCrypt, etc.
//    }
//}

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;

    public SecurityConfig(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // 1. PUBLIC ENDPOINTS (always allow) - UPDATED PATHS HERE!
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll() // <-- CHANGED
                        .requestMatchers("/logout").permitAll()

                        // 2. ADMIN ACCESS (highest privilege, placed early)
                        .requestMatchers("/**").hasRole("ADMIN") // This still works as a catch-all for ADMIN

                        // 3. OTHER SPECIFIC ROLE-BASED ACCESS (only apply if NOT ADMIN) - UPDATED PATHS HERE!
                        .requestMatchers("/api/products/**", "/api/categories/**").hasAnyRole("BUYER", "SELLER")
                        .requestMatchers("/api/seller/dashboard/**", "/api/seller/products/**").hasRole("SELLER")
                        .requestMatchers("/api/users/**").authenticated() // <-- CHANGED - Any authenticated user (excluding register/login)

                        // 4. CATCH-ALL for remaining (deny anything not explicitly allowed)
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}