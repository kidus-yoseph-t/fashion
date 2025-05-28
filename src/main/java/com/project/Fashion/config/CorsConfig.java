package com.project.Fashion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.*;

@Configuration
public class CorsConfig {
    public static final String X_REQUESTED_WITH = "X-Requested-with";

    @Bean
    public CorsFilter corsFilter(){
        var urlBasedConfigurationSource = new UrlBasedCorsConfigurationSource();
        var corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedOrigins(List.of("http://localhost:5173"));

        corsConfiguration.setAllowedHeaders(List.of(ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN, CONTENT_TYPE, ACCEPT, AUTHORIZATION, X_REQUESTED_WITH,ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_ALLOW_CREDENTIALS ));
        corsConfiguration.setExposedHeaders(List.of(ORIGIN, ACCESS_CONTROL_ALLOW_ORIGIN, CONTENT_TYPE, ACCEPT, AUTHORIZATION, X_REQUESTED_WITH,ACCESS_CONTROL_REQUEST_METHOD, ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_ALLOW_CREDENTIALS ));
        corsConfiguration.setAllowedMethods(List.of(GET.name(), POST.name(), PUT.name(), PATCH.name(), DELETE.name(), OPTIONS.name()));
        urlBasedConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter((urlBasedConfigurationSource));
    }
}
