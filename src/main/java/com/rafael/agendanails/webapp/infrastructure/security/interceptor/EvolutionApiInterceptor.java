package com.rafael.agendanails.webapp.infrastructure.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvolutionApiInterceptor implements HandlerInterceptor {

    private final String EVO_WEBHOOK_ENDPOINT = "/api/v1/webhook";
    @Value("${evolution.apikey}")
    private String evolutionApiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();

        if (path.startsWith(EVO_WEBHOOK_ENDPOINT)) {
            String apiKey = request.getHeader("apiKey");
            
            log.debug("Evolution Webhook Authentication - Path: {}, Received apiKey: {}", path, (apiKey != null ? "present" : "missing"));

            if (apiKey == null || !evolutionApiKey.equalsIgnoreCase(apiKey)) {
                log.warn("Evolution Webhook Authentication FAILED - Path: {}, Received apiKey: {}", path, apiKey);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            
            log.info("Evolution Webhook Authentication SUCCESS - Path: {}", path);
        }
        return true;
    }
}
