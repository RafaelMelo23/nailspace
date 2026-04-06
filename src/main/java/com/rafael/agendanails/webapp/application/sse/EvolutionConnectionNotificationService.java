package com.rafael.agendanails.webapp.application.sse;

import com.rafael.agendanails.webapp.domain.enums.SseEventType;
import com.rafael.agendanails.webapp.infrastructure.dto.sse.SsePayloadDTO;
import com.rafael.agendanails.webapp.infrastructure.sse.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvolutionConnectionNotificationService {

    private final SseService sseService;

    public void notifyQrCodeUpdate(Long professionalId, Object data) {
        SsePayloadDTO payload = SsePayloadDTO.builder()
                .sseEventType(SseEventType.QR_CODE_UPDATE)
                .data(data)
                .build();

        sseService.sendEventToUser(professionalId, payload);
    }

    public void notifyInstanceDisconnected(Long professionalId, Object data) {
        SsePayloadDTO payload = SsePayloadDTO.builder()
                .sseEventType(SseEventType.CONNECTION_UPDATE)
                .data(data)
                .build();

        sseService.sendEventToUser(professionalId, payload);
    }

    public void notifyInstanceConnected(Long professionalId, Object data) {
        SsePayloadDTO payload = SsePayloadDTO.builder()
                .sseEventType(SseEventType.CONNECTION_UPDATE)
                .data(data)
                .build();

        sseService.sendEventToUser(professionalId, payload);
    }
}