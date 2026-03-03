package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
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
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        int cost = 12;
        return new BCryptPasswordEncoder(cost);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilter securityFilter(TokenService tokenService) {
        return new SecurityFilter(tokenService);
    }

    // @Bean
    // public TenantStatusFilter tenantStatusFilter(SalonProfileService salonProfileService) {
    //     return new TenantStatusFilter(salonProfileService);
    // }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityFilter securityFilter
                                                   ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/v1/auth/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/webhook/**").permitAll()

                        .requestMatchers("/api/internal/**").hasRole("SUPER_ADMIN")

                        // Admin Endpoints
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Professional Endpoints
                        .requestMatchers("/api/v1/professional/**").hasAnyRole("PROFESSIONAL", "ADMIN")

                        // Client Endpoints
                        .requestMatchers("/api/v1/user/**").authenticated()
                        .requestMatchers("/api/v1/booking/**").authenticated()

                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
             // .addFilterAfter(tenantStatusFilter, SecurityFilter.class);

        return http.build();
    }
}