//package com.project.Fashion.config;//package com.project.Fashion.config;
//
////import org.springframework.context.annotation.Bean;
////import org.springframework.context.annotation.Configuration;
////import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
////import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // For @PreAuthorize
////import org.springframework.security.config.annotation.web.builders.HttpSecurity;
////import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
////import org.springframework.security.config.http.SessionCreationPolicy;
////import org.springframework.security.core.userdetails.UserDetailsService;
////import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.security.web.SecurityFilterChain;
////
////@Configuration
////@EnableWebSecurity // Enables Spring Security's web security features
////@EnableMethodSecurity(prePostEnabled = true) // Enables @PreAuthorize, @PostAuthorize etc.
////public class SecurityConfig {
////
////    private final UserDetailsService userDetailsService; // Your UserDetailsServiceImpl
////
////    // Inject UserDetailsService (e.g., from your security/UserDetailsServiceImpl.java)
////    public SecurityConfig(UserDetailsService userDetailsService) {
////        this.userDetailsService = userDetailsService;
////    }
////
////    @Bean
////    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
////        http
////                .csrf(csrf -> csrf.disable()) // Disable CSRF for API-only if you don't handle it. For web apps, you should enable and handle it.
////                .authorizeHttpRequests(authorize -> authorize
////                        // Public endpoints - accessible without authentication
////                        .requestMatchers("/login", "/register", "/api/auth/**").permitAll()
////                        // Specific role-based access for authenticated users
////                        .requestMatchers("/api/products/**", "/api/categories/**").hasAnyRole("BUYER", "SELLER")
////                        .requestMatchers("/api/seller/dashboard/**", "/api/seller/products/**").hasRole("SELLER")
////                        // All other requests require authentication
////                        .anyRequest().authenticated()
////                )
////                .formLogin(form -> form
////                        .loginPage("/login") // Specify your custom login page URL (if you have one)
////                        .loginProcessingUrl("/perform_login") // The URL to which the login form data is submitted
////                        .defaultSuccessUrl("/home", true) // Redirect after successful login (true forces redirection even if user tries to access a different page)
////                        .failureUrl("/login?error") // Redirect after failed login
////                        .permitAll() // Allow everyone to access the login page and process URL
////                )
////                .logout(logout -> logout
////                        .logoutUrl("/logout") // The URL to trigger logout
////                        .logoutSuccessUrl("/login?logout") // Redirect after successful logout
////                        .invalidateHttpSession(true) // Invalidate the HTTP session
////                        .deleteCookies("JSESSIONID") // Delete the session cookie
////                        .permitAll() // Allow everyone to access the logout URL
////                )
////                .sessionManagement(session -> session
////                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Spring will create a session if one doesn't exist
////                        .maximumSessions(1) // Limit concurrent sessions for a user
////                        .maxSessionsPreventsLogin(true) // If a new login attempt occurs when max sessions are reached, prevent the new login
////                );
////
////        return http.build();
////    }
////
////    // Configure the AuthenticationProvider to use your UserDetailsService and PasswordEncoder
////    @Bean
////    public DaoAuthenticationProvider daoAuthenticationProvider() {
////        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
////        provider.setUserDetailsService(userDetailsService); // Inject your UserDetailsService
////        provider.setPasswordEncoder(passwordEncoder()); // Use the PasswordEncoder bean
////        return provider;
////    }
////
////    // Define your PasswordEncoder bean
////    @Bean
////    public PasswordEncoder passwordEncoder() {
////        return new BCryptPasswordEncoder(); // Or Argon2, SCrypt, etc.
////    }
////}
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity(prePostEnabled = true)
//public class SecurityConfig {
//
//    private final UserDetailsService userDetailsService;
//
//    public SecurityConfig(UserDetailsService userDetailsService) {
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(authorize -> authorize
//                        // 1. PUBLIC ENDPOINTS (always allow)
//                        .requestMatchers("/api/users/register", "/api/users/login").permitAll()
//                        .requestMatchers("/api/logout").permitAll() // Ensure logout is also public
//
//                        // 2. ADMIN ACCESS (highest privilege, placed early)
//                        // If you want ADMIN to access EVERYTHING that is not 'permitAll()':
//                        .requestMatchers("/**").hasRole("ADMIN") // ADMIN can access ANY path (except /users/register, /users/login, /logout)
//
//                        // 3. OTHER SPECIFIC ROLE-BASED ACCESS (these rules will only apply if the user is NOT ADMIN)
//                        .requestMatchers("/api/products/**", "/api/categories/**").hasAnyRole("BUYER", "SELLER")
//                        .requestMatchers("/api/seller/dashboard/**", "/api/seller/products/**").hasRole("SELLER")
//                        .requestMatchers("/users/**").authenticated() // Any authenticated user (excluding register/login)
//
//                        // 4. CATCH-ALL for remaining authenticated paths
//                        // This rule is now effectively redundant if '/**'.hasRole("ADMIN") is placed first
//                        // and you want ADMINs to access everything.
//                        // If you want *any* logged-in user (not Admin) to access other unspecified paths,
//                        // this should be placed after the role-specific ones but before 'denyAll()'.
//                        // However, with '/**'.hasRole("ADMIN"), any non-admin reaching here would be denied.
//                        // You might need to adjust based on exact desired hierarchy.
//                        // For a typical setup where ADMIN "sees all" and others have limited views:
//                        // .anyRequest().authenticated() // This would now only apply to non-admin roles
//                        // if they get past previous rules.
//                        // It might be better to explicitly deny if not matched.
//                        .anyRequest().denyAll() // Deny any request not explicitly permitted or assigned a role
//                )
//                .formLogin(form -> form.disable())
//                .httpBasic(httpBasic -> httpBasic.disable())
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
//                        .maximumSessions(1)
//                        .maxSessionsPreventsLogin(true)
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
//                        .invalidateHttpSession(true)
//                        .deleteCookies("JSESSIONID")
//                        .permitAll()
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public DaoAuthenticationProvider daoAuthenticationProvider() {
//        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//        provider.setUserDetailsService(userDetailsService);
//        provider.setPasswordEncoder(passwordEncoder());
//        return provider;
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
//        return authenticationConfiguration.getAuthenticationManager();
//    }
//}