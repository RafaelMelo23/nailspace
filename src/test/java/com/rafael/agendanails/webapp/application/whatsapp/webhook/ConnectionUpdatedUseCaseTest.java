package com.rafael.agendanails.webapp.application.whatsapp.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.application.sse.EvolutionConnectionNotificationService;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.connection.ConnectionDataDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionUpdatedUseCaseTest {

    @Mock
    private SalonProfileService salonProfileService;
    @Mock
    private WhatsappProvider whatsappProvider;
    @Mock
    private EvolutionConnectionNotificationService connectionNotificationService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConnectionUpdatedUseCase connectionUpdatedUseCase;

    private SalonProfile salon;
    private Professional owner;
    private final String tenantId = "tenant-1";

    @BeforeEach
    void setUp() {
        owner = Professional.builder().id(1L).build();
        salon = SalonProfile.testBuilder()
                .tenantId(tenantId)
                .owner(owner)
                .build();
    }

    @Test
    void shouldHandleOpenConnection() {
        ConnectionDataDTO data = new ConnectionDataDTO(EvolutionConnectionState.OPEN);
        EvolutionWebhookResponseDTO<ConnectionDataDTO> response = EvolutionWebhookResponseDTO.<ConnectionDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService).save(salon);
        verify(connectionNotificationService).notifyInstanceConnected(eq(1L), anyString());
        assert salon.getEvolutionConnectionState() == EvolutionConnectionState.OPEN;
    }

    @Test
    void shouldHandleCloseConnection() {
        ConnectionDataDTO data = new ConnectionDataDTO(EvolutionConnectionState.CLOSE);
        EvolutionWebhookResponseDTO<ConnectionDataDTO> response = EvolutionWebhookResponseDTO.<ConnectionDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService).save(salon);
        verify(connectionNotificationService).notifyInstanceDisconnected(eq(1L), anyString());
        verify(whatsappProvider).deleteInstance(tenantId);
        assert salon.getEvolutionConnectionState() == EvolutionConnectionState.CLOSE;
        assert salon.getWhatsappLastResetAt() != null;
    }

    @Test
    void shouldProcessOpenEventEvenUnderCooldown() {
        salon.updateWhatsAppConnection(null, LocalDateTime.now().minusSeconds(10), null);
        ConnectionDataDTO data = new ConnectionDataDTO(EvolutionConnectionState.OPEN);
        
        EvolutionWebhookResponseDTO<ConnectionDataDTO> response = EvolutionWebhookResponseDTO.<ConnectionDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService).save(salon);
        verify(connectionNotificationService).notifyInstanceConnected(eq(1L), anyString());
        assert salon.getEvolutionConnectionState() == EvolutionConnectionState.OPEN;
    }

    @Test
    void shouldIgnoreDuplicateCloseUnderCooldown() {
        salon.updateWhatsAppConnection(EvolutionConnectionState.CLOSE, LocalDateTime.now().minusMinutes(1), null);
        ConnectionDataDTO data = new ConnectionDataDTO(EvolutionConnectionState.CLOSE);
        EvolutionWebhookResponseDTO<ConnectionDataDTO> response = EvolutionWebhookResponseDTO.<ConnectionDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService, never()).save(any());
        verify(whatsappProvider, never()).deleteInstance(anyString());
    }

    @Test
    void shouldProcessCloseEvenUnderCooldownIfPreviouslyOpen() {
        salon.updateWhatsAppConnection(EvolutionConnectionState.OPEN, LocalDateTime.now().minusMinutes(1), null);
        ConnectionDataDTO data = new ConnectionDataDTO(EvolutionConnectionState.CLOSE);
        EvolutionWebhookResponseDTO<ConnectionDataDTO> response = EvolutionWebhookResponseDTO.<ConnectionDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService).save(salon);
        verify(connectionNotificationService).notifyInstanceDisconnected(eq(1L), anyString());
        verify(whatsappProvider).deleteInstance(tenantId);
        assert salon.getEvolutionConnectionState() == EvolutionConnectionState.CLOSE;
    }

    @Test
    void shouldConvertMapToDtoIfNecessary() {
        Map<String, Object> dataMap = Map.of("state", "OPEN");
        ConnectionDataDTO dataDto = new ConnectionDataDTO(EvolutionConnectionState.OPEN);
        
        EvolutionWebhookResponseDTO<Map<String, Object>> response = EvolutionWebhookResponseDTO.<Map<String, Object>>builder()
                .instance(tenantId)
                .data(dataMap)
                .build();

        when(objectMapper.convertValue(dataMap, ConnectionDataDTO.class)).thenReturn(dataDto);
        when(salonProfileService.findWithOwnerByTenantId(tenantId)).thenReturn(salon);

        connectionUpdatedUseCase.process(response);

        verify(salonProfileService).save(salon);
        assert salon.getEvolutionConnectionState() == EvolutionConnectionState.OPEN;
    }
}
