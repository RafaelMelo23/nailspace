package com.rafael.nailspro.webapp.application.whatsapp.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.application.sse.EvolutionConnectionNotificationService;
import com.rafael.nailspro.webapp.domain.webhook.WebhookStrategy;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.connection.ConnectionDataDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState.CLOSE;
import static com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState.OPEN;

@Slf4j
@Service
@RequiredArgsConstructor
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

                if (isUnderCooldown(salon)) {
                    log.warn("Ignoring close for tenant: {} - Instance is in pairing", tenantId);
                    return;
                }

                switch (state) {
                    case OPEN -> {
                        if (shouldIgnoreClose(salon)) {
                            log.info("Ignoring inconsistent CLOSE event for tenant: {}", tenantId);
                            return;
                        }
                        handleOpenConnection(salon);
                    }
                    case CLOSE -> handleDisconnection(salon);
                    case CONNECTING -> {
                    }
                }
            }
        }
    }

    private boolean shouldIgnoreClose(SalonProfile salon) {
        if (salon.getWhatsappLastResetAt() == null) return false;

        return salon.getWhatsappLastResetAt()
                .isAfter(LocalDateTime.now().minusSeconds(45));
    }

    private boolean isUnderCooldown(SalonProfile salon) {
        if (salon.getWhatsappLastResetAt() == null) return false;

        return salon.getWhatsappLastResetAt()
                .isAfter(LocalDateTime.now().minusMinutes(2));
    }

    private void handleOpenConnection(SalonProfile salon) {
        String tenantId = salon.getTenantId();
        Long ownerId = salon.getOwner().getId();

        salon.setEvolutionConnectionState(OPEN);
        salonProfileService.save(salon);

        connectionNotificationService.notifyInstanceDisconnected(
                ownerId,
                "Whatsapp instance connected");

        log.info("SSE notification sent to owner ID: {} regarding instance {} connection.", ownerId, tenantId);
    }

    private void handleDisconnection(SalonProfile salon) {
        String tenantId = salon.getTenantId();
        Long ownerId = salon.getOwner().getId();

        salon.setEvolutionConnectionState(CLOSE);
        salon.setWhatsappLastResetAt(LocalDateTime.now());
        salonProfileService.save(salon);

        connectionNotificationService.notifyInstanceDisconnected(
                ownerId,
                "Whatsapp instance disconnected");

        log.info("SSE notification sent to owner ID: {} regarding instance {} disconnection.", ownerId, tenantId);

        whatsappProvider.deleteInstance(tenantId);
        log.info("Instance {} successfully deleted from Evolution API to prevent inconsistent connection states.", tenantId);
    }

    @Override
    public String getSupportedTypeEvent() {
        return EvolutionWebhookEvent.CONNECTION_UPDATE.toString();
    }
}