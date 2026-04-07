package com.rafael.agendanails.webapp.infrastructure.security.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String traceId = MDC.get("trace_id");
        if (traceId == null) traceId = "N/A";

        log.warn("401 Unauthorized: method={} path={} traceId={} reason={}",
                request.getMethod(), request.getRequestURI(), traceId, authException.getMessage());

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                request.getRequestDispatcher("/error").forward(request, response);
            } catch (Exception e) {
                log.error("Error forwarding to /error", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "messages", List.of("Authentication required."),
                "path", request.getRequestURI(),
                "traceId", traceId
        ));
    }
}
