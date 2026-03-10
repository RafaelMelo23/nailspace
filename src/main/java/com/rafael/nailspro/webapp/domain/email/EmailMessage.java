package com.rafael.nailspro.webapp.domain.email;

import lombok.Builder;

@Builder
public record EmailMessage(
        String to,
        String subject,
        String body
) {
}
