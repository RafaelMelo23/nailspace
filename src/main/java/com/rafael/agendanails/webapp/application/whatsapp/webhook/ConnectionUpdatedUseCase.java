package com.rafael.agendanails.webapp.application.whatsapp.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.application.sse.EvolutionConnectionNotificationService;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.webhook.WebhookStrategy;
import com.rafael.agendanails.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.connection.ConnectionDataDTO;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState.CLOSE;
import static com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState.OPEN;

@Slf4j
@Service
@RequiredArgsConstructor
@IgnoreTenantFilter
public class ConnectionUpdatedUseCase implements WebhookStrategy {

    private final SalonProfileService salonProfileService;
    private final WhatsappProvider whatsappProvider;
    private final EvolutionConnectionNotificationService connectionNotificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void process(Object payload) {
        if (payload instanceof EvolutionWebhookResponseDTO<?> response) {
            Object data = response.data();
            String tenantId = response.instance();

            if (data instanceof java.util.Map) {
                data = objectMapper.convertValue(data, ConnectionDataDTO.class);
            }

            if (data instanceof ConnectionDataDTO(
                    EvolutionConnectionState state
            )) {
                log.info("Successfully converted LinkedHashMap to ConnectionData for instance: {}", response.instance());
                SalonProfile salon = salonProfileService.findWithOwnerByTenantId(tenantId);

                switch (state) {
                    case OPEN -> handleOpenConnection(salon);
                    case CLOSE -> {
                        if (isUnderCooldown(salon)) {
                            log.warn("Ignoring CLOSE for tenant: {} - Instance is in pairing/reset cooldown and already CLOSED", tenantId);
                            return;
                        }
                        handleDisconnection(salon);
                    }
                    case CONNECTING -> {
                    }
                }
            }
        }
    }

    private boolean isUnderCooldown(SalonProfile salon) {
        if (salon.getWhatsappLastResetAt() == null) return false;

        boolean isRecentReset = salon.getWhatsappLastResetAt()
                .isAfter(LocalDateTime.now().minusMinutes(2));

        return isRecentReset &&
                (salon.getEvolutionConnectionState() == null || salon.getEvolutionConnectionState() == CLOSE);
    }

    private void handleOpenConnection(SalonProfile salon) {
        String tenantId = salon.getTenantId();
        Long ownerId = salon.getOwner().getId();

        salon.updateWhatsAppConnection(OPEN, null, null);
        salonProfileService.save(salon);

        try {
            connectionNotificationService.notifyInstanceConnected(
                    ownerId,
                    OPEN.name());
            log.info("SSE notification sent to owner ID: {} regarding instance {} connection.", ownerId, tenantId);
        } catch (Exception e) {
            log.error("Failed to send SSE notification for instance {}: {}", tenantId, e.getMessage());
        }
    }

    private void handleDisconnection(SalonProfile salon) {
        String tenantId = salon.getTenantId();
        Long ownerId = salon.getOwner().getId();

        salon.updateWhatsAppConnection(CLOSE, LocalDateTime.now(), null);
        salonProfileService.save(salon);

        try {
            connectionNotificationService.notifyInstanceDisconnected(
                    ownerId,
                    CLOSE.name());
            log.info("SSE notification sent to owner ID: {} regarding instance {} disconnection.", ownerId, tenantId);
        } catch (Exception e) {
            log.error("Failed to send SSE notification for instance {}: {}", tenantId, e.getMessage());
        }

        try {
            whatsappProvider.deleteInstance(tenantId);
            log.info("Instance {} successfully deleted from Evolution API.", tenantId);
        } catch (Exception e) {
            log.error("Critical: Failed to delete instance {} from provider: {}", tenantId, e.getMessage());
        }
    }

    @Override
    public String getSupportedTypeEvent() {
        return EvolutionWebhookEvent.CONNECTION_UPDATE.toString();
    }
}