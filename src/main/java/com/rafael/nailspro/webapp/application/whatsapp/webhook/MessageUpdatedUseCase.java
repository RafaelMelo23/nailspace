package com.rafael.nailspro.webapp.application.whatsapp.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationStatus;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.nailspro.webapp.domain.repository.AppointmentNotificationRepository;
import com.rafael.nailspro.webapp.domain.webhook.WebhookStrategy;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.message.update.MessageUpdateData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageUpdatedUseCase implements WebhookStrategy {

    private final ObjectMapper objectMapper;
    private final AppointmentNotificationRepository notificationRepository;

    @Override
    public void process(Object payload) {
        extractWebhookResponse(payload)
                .ifPresent(this::processWebhookResponse);
    }

    private Optional<EvolutionWebhookResponseDTO<?>> extractWebhookResponse(Object payload) {
        if (payload instanceof EvolutionWebhookResponseDTO<?> response) {
            log.info("Received MESSAGE_UPDATE event for instance: {}", response.instance());
            return Optional.of(response);
        }
        return Optional.empty();
    }

    private void processWebhookResponse(EvolutionWebhookResponseDTO<?> response) {
        convertDataToMessageUpdateData(response.data())
                .ifPresent(this::updateNotificationStatus);
    }

    @Transactional
    protected void updateNotificationStatus(MessageUpdateData updateData) {
        notificationRepository.findByExternalMessageId(updateData.messageId())
                .ifPresent(n -> {
                            n.setNotificationStatus(
                                    AppointmentNotificationStatus.fromEvolutionStatus(updateData.status())
                            );
                            log.debug("Updated notification status for message ID: {} to: {}", n.getExternalMessageId(), n.getNotificationStatus());
                            notificationRepository.save(n);
                        }
                );
    }

    private Optional<MessageUpdateData> convertDataToMessageUpdateData(Object data) {
        if (data instanceof java.util.Map) {
            data = objectMapper.convertValue(data, MessageUpdateData.class);
        }

        if (data instanceof MessageUpdateData updateData) {
            return Optional.of(updateData);
        }
        return Optional.empty();
    }

    @Override
    public String getSupportedTypeEvent() {
        return EvolutionWebhookEvent.MESSAGE_UPDATE.toString();
    }
}