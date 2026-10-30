
package com.jobportal.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            // Apply CORS before Spring Security authorization
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource)
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(
                    HttpMethod.OPTIONS, "/**"
                ).permitAll()

                .requestMatchers(
                    "/", "/health", "/error",
                    "/actuator/health"
                ).permitAll()

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/jobs",
                    "/api/jobs/**"
                ).permitAll()

                .requestMatchers(
                    "/api/auth/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                .anyRequest().authenticated()
            )

            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:https://jobhire-web-ul1c.onrender.com,http://localhost:5173,http://127.0.0.1:5173}")
            String allowedOrigins) {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            Arrays.stream(allowedOrigins.split("\\s*,\\s*"))
                .filter(origin -> !origin.isBlank())
                .toList()
        );

        configuration.setAllowedMethods(
            List.of(
                "GET", "POST", "PUT",
                "PATCH", "DELETE", "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of("*")
        );

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**", configuration
        );

        return source;
    }
}
