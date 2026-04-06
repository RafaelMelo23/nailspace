package com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebhookDTO(
        boolean enabled,
        String url,
        boolean byEvents,
        boolean base64,
        List<EvolutionWebhookEvent> events,
        Map<String, String> headers
) {
}