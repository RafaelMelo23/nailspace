package com.rafael.nailspro.webapp.application.whatsapp.webhook;

import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.nailspro.webapp.domain.webhook.WebhookStrategy;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        WebhookStrategy handler = strategyMap.get(webhookDTO.event());

        handler.process(webhookDTO);
    }
}