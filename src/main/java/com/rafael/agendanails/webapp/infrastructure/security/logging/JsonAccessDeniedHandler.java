package com.rafael.agendanails.webapp.infrastructure.security.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        String traceId = MDC.get("trace_id");
        if (traceId == null) traceId = "N/A";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = auth != null ? auth.getName() : "anonymous";

        log.warn("403 Forbidden: method={} path={} user={} traceId={} reason={}",
                request.getMethod(), request.getRequestURI(), principal, traceId, ex.getMessage());

        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/html")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try {
                request.getRequestDispatcher("/error").forward(request, response);
            } catch (Exception e) {
                log.error("Error forwarding to /error", e);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, ex.getMessage());
            }
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "messages", List.of("Insufficient permissions."),
                "path", request.getRequestURI(),
                "traceId", traceId
        ));
    }
}

