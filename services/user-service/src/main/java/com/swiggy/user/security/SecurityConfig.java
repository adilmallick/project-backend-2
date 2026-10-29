package com.swiggy.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration for user-service.
 *
 * <p>DECISION: Stateless security architecture.
 *
 * <p>WHY CSRF IS DISABLED:
 * CSRF (Cross-Site Request Forgery) attacks exploit automatic browser cookie transmission.
 * Because this service uses stateless Bearer JWT headers and stores no session cookies,
 * CSRF protection is unnecessary and disabled.
 *
 * <p>SEE: ADR-003
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // DECISION: Disable CSRF for stateless REST APIs using Bearer tokens.
                .csrf(AbstractHttpConfigurer::disable)

                // DECISION: Enforce SessionCreationPolicy.STATELESS to ensure no HTTP sessions are created in memory.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // DECISION: Public routes for authentication and health probes; all domain APIs require authentication.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                // DECISION: Insert JWT filter before the standard UsernamePasswordAuthenticationFilter.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // DECISION: BCrypt with work factor 12 provides robust protection against GPU-based brute force cracking.
        // TRADE-OFF: Work factor 12 costs ~250ms per hash computation, deliberately slowing down brute force attempts.
        return new BCryptPasswordEncoder(12);
    }
}
