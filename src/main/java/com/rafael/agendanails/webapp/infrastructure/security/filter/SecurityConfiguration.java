package com.rafael.agendanails.webapp.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.agendanails.webapp.infrastructure.security.logging.JsonAccessDeniedHandler;
import com.rafael.agendanails.webapp.infrastructure.security.logging.JsonAuthenticationEntryPoint;
import com.rafael.agendanails.webapp.infrastructure.security.token.TokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantResolver;
import lombok.RequiredArgsConstructor;
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            config.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        } else {
            config.setAllowedOriginPatterns(List.of(
                    "http://*.localhost:8080",
                    "http://localhost:8080"
            )); // todo: alter for prod
        }

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   TokenService tokenService,
                                                   TenantResolver tenantResolver,
                                                   JsonAuthenticationEntryPoint authenticationEntryPoint,
                                                   JsonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        SecurityFilter securityFilter = new SecurityFilter(tokenService, tenantResolver);
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

                        // ===== ADMIN HTML PAGES (PROTECTED BY JS/API) =====
                        
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
                                "/api/v1/whatsapp/**"
                        ).hasAnyRole("SUPER_ADMIN", "ADMIN")

                        // ===== AUTHENTICATED USERS (CLIENT AREA) =====
                        .requestMatchers(
                                "/api/v1/user/**",
                                "/api/v1/booking/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}