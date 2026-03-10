package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.nailspro.webapp.infrastructure.security.logging.JsonAccessDeniedHandler;
import com.rafael.nailspro.webapp.infrastructure.security.logging.JsonAuthenticationEntryPoint;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final ObjectMapper mapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        int cost = 12;
        return new BCryptPasswordEncoder(cost);
    }

    @Bean
    public JsonAuthenticationEntryPoint authenticationEntryPoint() {
        return new JsonAuthenticationEntryPoint(mapper);
    }

    @Bean
    public JsonAccessDeniedHandler accessDeniedHandler() {
        return new JsonAccessDeniedHandler(mapper);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilter securityFilter(TokenService tokenService) {
        return new SecurityFilter(tokenService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityFilter securityFilter,
                                                   JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler
                                                   ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/webhook/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/internal/**").hasRole("SUPER_ADMIN")

                        // Admin Endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Professional Endpoints
                        .requestMatchers("/api/v1/professional/**").hasAnyRole("PROFESSIONAL", "ADMIN")

                        .requestMatchers("/api/v1/whatsapp/**").hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // Client Endpoints
                        .requestMatchers("/api/v1/user/**").authenticated()
                        .requestMatchers("/api/v1/booking/**").authenticated()

                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}