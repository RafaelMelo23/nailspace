package com.rafael.nailspro.webapp.application.whatsapp;

import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageStatus;
import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageType;
import com.rafael.nailspro.webapp.domain.model.WhatsappMessage;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.domain.repository.RetentionForecastRepository;
import com.rafael.nailspro.webapp.domain.repository.WhatsappMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WhatsappMessageService {

    private final WhatsappMessageRepository messageRepository;
    private final AppointmentRepository appointmentRepository;
    private final RetentionForecastRepository retentionForecastRepository;

    @Transactional
    public WhatsappMessage prepareAppointmentMessage(Long appointmentId, WhatsappMessageType type) {
        return messageRepository
                .findByAppointmentIdAndMessageType(appointmentId, type)
                .orElseGet(() -> {
                    var appointment = appointmentRepository.findById(appointmentId)
                            .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

                    var newMessage = WhatsappMessage.builder()
                            .appointment(appointment)
                            .destinationNumber(appointment.getClient().getPhoneNumber())
                            .messageType(type)
                            .messageStatus(WhatsappMessageStatus.FAILED)
                            .attempts(0)
                            .tenantId(appointment.getTenantId())
                            .build();

                    return messageRepository.save(newMessage);
                });
    }

    @Transactional
    public WhatsappMessage prepareRetentionMessage(Long retentionForecastId, WhatsappMessageType type) {
        return messageRepository
                .findByRetentionForecastIdAndMessageType(retentionForecastId, type)
                .orElseGet(() -> {
                    var retentionForecast = retentionForecastRepository.findById(retentionForecastId)
                            .orElseThrow(() -> new IllegalArgumentException("Retention forecast not found"));

                    var newMessage = WhatsappMessage.builder()
                            .retentionForecast(retentionForecast)
                            .destinationNumber(retentionForecast.getClient().getPhoneNumber())
                            .messageType(type)
                            .messageStatus(WhatsappMessageStatus.FAILED)
                            .attempts(0)
                            .tenantId(retentionForecast.getTenantId())
                            .build();

                    return messageRepository.save(newMessage);
                });
    }

    @Transactional
    public void updateMessageStatus(WhatsappMessageStatus status,
                                    String error,
                                    String externalMessageId,
                                    Long id) {

        messageRepository.findById(id).ifPresent(m -> {
            m.setMessageStatus(status);
            m.setExternalMessageId(externalMessageId);
            m.setAttempts(m.getAttempts() + 1);
            m.setLastAttemptAt(Instant.now());
            m.setLastErrorMessage(error);
            if (status == WhatsappMessageStatus.SENT) {
                m.setSentAt(Instant.now());
            }
        });
    }
}