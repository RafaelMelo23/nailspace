package com.rafael.nailspro.webapp.infrastructure.dto.email;

import lombok.Builder;

@Builder
public record ForgotPasswordEmailDTO(
        String userEmail,
        String resetLink,
        String tenantName
) {}