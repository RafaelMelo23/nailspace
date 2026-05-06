package com.rafael.agendanails.webapp.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.agendanails.webapp.infrastructure.security.filter.JwtAuthenticationFilter;
import com.rafael.agendanails.webapp.infrastructure.security.handler.ApiAccessDeniedHandler;
import com.rafael.agendanails.webapp.infrastructure.security.handler.ApiAuthenticationEntryPoint;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public ApiAuthenticationEntryPoint authenticationEntryPoint() {
        return new ApiAuthenticationEntryPoint(mapper);
    }

    @Bean
    public ApiAccessDeniedHandler accessDeniedHandler() {
        return new ApiAccessDeniedHandler(mapper);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Value("${domain.url:}")
    private String domainUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>();
        
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            origins.addAll(Arrays.asList(allowedOrigins.split(",")));
        } else {
            origins.add("http://localhost:8080");
            origins.add("http://127.0.0.1:8080");
            
            if (domainUrl != null && !domainUrl.isEmpty()) {
                origins.add(domainUrl);

                if (domainUrl.endsWith("/")) {
                    origins.add(domainUrl.substring(0, domainUrl.length() - 1));
                }
            }
        }

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Tenant-Id", "X-XSRF-TOKEN", "Accept", "Origin"));
        config.setExposedHeaders(List.of("X-Salon-State"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtTokenService jwtTokenService,
                                                   TenantResolver tenantResolver,
                                                   ApiAuthenticationEntryPoint authenticationEntryPoint,
                                                   ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService, tenantResolver);
        http.cors(cors -> {
                })
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesh -> sesh.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ===== PUBLIC =====
                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/webhook",
                                "/api/v1/webhook/**",
                                "/api/v1/professional/simplified",
                                "/api/v1/booking/{professionalExternalId}/availability",
                                "/error/**",
                                "/uploads/**",
                                "/offline"
                        ).permitAll()

                        .requestMatchers(HttpMethod.GET, 
                                "/api/v1/salon/service",
                                "/api/v1/salon/profile").permitAll()

                        // ===== PUBLIC HTML PAGES =====
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/index.html",
                                "/favicon.svg",
                                "/favicon.ico",
                                "/assets/**",
                                "/css/**",
                                "/js/**",
                                "/pages/**",
                                "/agendar",
                                "/entrar",
                                "/cadastro",
                                "/perfil",
                                "/manutencao",
                                "/redefinir-senha",
                                "/admin/servicos",
                                "/admin/configuracoes",
                                "/profissional/agenda").permitAll()

                        // ===== SWAGGER (RESTRICTED TO SUPER_ADMIN) =====
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**"
                        ).hasAuthority("SUPER_ADMIN")

                        // ===== SUPER ADMIN =====
                        .requestMatchers(
                                "/api/internal/**"
                        ).hasRole("SUPER_ADMIN")

                        // ===== ADMIN =====
                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).hasRole("ADMIN")

                        // ===== PROFESSIONAL + ADMIN =====
                        .requestMatchers(
                                "/api/v1/professional/**"
                        ).hasAnyRole("PROFESSIONAL", "ADMIN")

                        // ===== ADMIN + SUPER ADMIN =====
                        .requestMatchers(
                                "/api/v1/whatsapp/**",
                                "/api/v1/notifications"
                        ).hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // ===== AUTHENTICATED USERS (CLIENT AREA) =====
                        .requestMatchers(
                                "/api/v1/user/**",
                                "/api/v1/booking/**"
                        ).authenticated()

                        .requestMatchers(HttpMethod.GET,
                                "/{tenantId}",
                                "/{tenantId}/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}