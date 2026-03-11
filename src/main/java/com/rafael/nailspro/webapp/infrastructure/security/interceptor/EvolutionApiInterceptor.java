package com.rafael.nailspro.webapp.infrastructure.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class EvolutionApiInterceptor implements HandlerInterceptor {

    private final String EVO_WEBHOOK_ENDPOINT = "/api/v1/webhook";
    @Value("${evolution.apikey}")
    private String evolutionApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if (EVO_WEBHOOK_ENDPOINT.equalsIgnoreCase(path)) {
            String apiKey = request.getHeader("apiKey");

            if (!evolutionApiKey.equalsIgnoreCase(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return true;
            }
        }
        return false;
    }
}