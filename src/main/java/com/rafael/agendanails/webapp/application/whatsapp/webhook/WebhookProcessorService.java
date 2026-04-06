package com.rafael.agendanails.webapp.application.whatsapp.webhook;

import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.agendanails.webapp.domain.webhook.WebhookStrategy;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Service
public class WebhookProcessorService {

    private final Map<EvolutionWebhookEvent, WebhookStrategy> strategyMap;

    public WebhookProcessorService(List<WebhookStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        ws -> EvolutionWebhookEvent.valueOf(ws.getSupportedTypeEvent()), Function.identity()));
    }

    @Transactional
    public void handleWebhook(EvolutionWebhookResponseDTO<?> webhookDTO) {
        EvolutionWebhookEvent event = webhookDTO.event();
        WebhookStrategy handler = strategyMap.get(event);

        if (handler == null) {
            log.warn("No handler found for event type: {}", event);
            return;
        }

        log.info("Processing event: {} for instance: {}", event, webhookDTO.instance());
        handler.process(webhookDTO);
    }
}