package com.rafael.nailspro.webapp.application.whatsapp.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.nailspro.webapp.application.professional.ProfessionalQueryService;
import com.rafael.nailspro.webapp.application.sse.EvolutionConnectionNotificationService;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.connection.update.QrCodeDataDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.connection.update.QrCodeDetailsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeUpdatedUseCaseTest {

    @Mock
    private ProfessionalQueryService professionalQueryService;
    @Mock
    private EvolutionConnectionNotificationService connectionNotificationService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private QrCodeUpdatedUseCase qrCodeUpdatedUseCase;

    @Test
    void shouldHandleQrCodeUpdate() {
        String tenantId = "tenant-1";
        QrCodeDetailsDTO details = new QrCodeDetailsDTO("pairing-code", "qrcode-string", "base64-string");
        QrCodeDataDTO data = new QrCodeDataDTO(details);
        
        EvolutionWebhookResponseDTO<QrCodeDataDTO> response = EvolutionWebhookResponseDTO.<QrCodeDataDTO>builder()
                .instance(tenantId)
                .data(data)
                .build();

        Professional owner = Professional.builder().id(10L).build();
        when(professionalQueryService.findByTenantId(tenantId)).thenReturn(owner);

        qrCodeUpdatedUseCase.process(response);

        verify(connectionNotificationService).notifyQrCodeUpdate(10L, details);
    }

    @Test
    void shouldConvertMapToDtoIfNecessary() {
        String tenantId = "tenant-1";
        Map<String, Object> dataMap = Map.of("qrcode", Map.of("code", "qrcode-string"));
        QrCodeDetailsDTO details = new QrCodeDetailsDTO("pairing-code", "qrcode-string", "base64-string");
        QrCodeDataDTO dataDto = new QrCodeDataDTO(details);

        EvolutionWebhookResponseDTO<Map<String, Object>> response = EvolutionWebhookResponseDTO.<Map<String, Object>>builder()
                .instance(tenantId)
                .data(dataMap)
                .build();

        when(objectMapper.convertValue(dataMap, QrCodeDataDTO.class)).thenReturn(dataDto);
        
        Professional owner = Professional.builder().id(10L).build();
        when(professionalQueryService.findByTenantId(tenantId)).thenReturn(owner);

        qrCodeUpdatedUseCase.process(response);

        verify(connectionNotificationService).notifyQrCodeUpdate(10L, details);
    }
}
