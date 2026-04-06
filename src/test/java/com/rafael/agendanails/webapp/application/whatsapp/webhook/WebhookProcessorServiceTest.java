package com.rafael.agendanails.webapp.application.whatsapp.webhook;

import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.agendanails.webapp.domain.webhook.WebhookStrategy;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookProcessorServiceTest {

    private WebhookProcessorService webhookProcessorService;

    @Mock
    private WebhookStrategy strategy1;

    @Mock
    private WebhookStrategy strategy2;

    @BeforeEach
    void setUp() {
        when(strategy1.getSupportedTypeEvent()).thenReturn("MESSAGE_UPDATE");
        when(strategy2.getSupportedTypeEvent()).thenReturn("CONNECTION_UPDATE");

        webhookProcessorService = new WebhookProcessorService(List.of(strategy1, strategy2));
    }

    @Test
    void shouldDelegateToCorrectStrategy() {
        EvolutionWebhookResponseDTO<Object> dto = EvolutionWebhookResponseDTO.builder()
                .event(EvolutionWebhookEvent.MESSAGES_UPDATE)
                .instance("instance")
                .data(new Object())
                .build();

        webhookProcessorService.handleWebhook(dto);

        verify(strategy1).process(dto);
    }
}
