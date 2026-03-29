package com.rafael.nailspro.webapp.application.appointment.message;

import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageStatus;
import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageType;
import com.rafael.nailspro.webapp.domain.model.*;
import com.rafael.nailspro.webapp.domain.repository.WhatsappMessageRepository;
import com.rafael.nailspro.webapp.domain.whatsapp.SentMessageResult;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AppointmentMessagingUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private AppointmentMessagingUseCase appointmentMessagingUseCase;

    @Autowired
    private WhatsappMessageRepository whatsappMessageRepository;

    @MockitoBean
    private WhatsappProvider whatsappProvider;

    @Test
    void shouldProcessConfirmationMessageSuccessfullyWhenProviderReturnsSuccess() {
        String tenantId = "tenant-test";
        
        Professional prof = TestProfessionalFactory.standardForIt(tenantId);
        professionalRepository.save(prof);

        SalonProfile profile = TestSalonProfileFactory.standardForIT(prof, tenantId);
        salonProfileRepository.save(profile);

        Client client = TestClientFactory.standardForIt(tenantId);
        clientRepository.save(client);

        SalonService service = TestSalonServiceFactory.standardForIt(tenantId);
        salonServiceRepository.save(service);

        Appointment appointment = TestAppointmentFactory.standardForIt(client, prof, service);
        appointmentRepository.save(appointment);

        SentMessageResult result = SentMessageResult.builder()
                .messageId("ext-123")
                .status(com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionMessageStatus.SERVER_ACK)
                .build();
        when(whatsappProvider.sendText(eq(tenantId), anyString(), eq(client.getPhoneNumber())))
                .thenReturn(result);

        appointmentMessagingUseCase.sendAppointmentConfirmationMessage(appointment.getId());

        List<WhatsappMessage> messages = whatsappMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        WhatsappMessage msg = messages.get(0);
        assertThat(msg.getMessageStatus()).isEqualTo(WhatsappMessageStatus.SENT);
        assertThat(msg.getExternalMessageId()).isEqualTo("ext-123");
        assertThat(msg.getMessageType()).isEqualTo(WhatsappMessageType.CONFIRMATION);
        assertThat(msg.getAttempts()).isEqualTo(1);
    }

    @Test
    void shouldHandleFailedMessageDispatchWhenProviderThrowsException() {
        String tenantId = "tenant-test";

        Professional prof = TestProfessionalFactory.standardForIt(tenantId);
        professionalRepository.save(prof);

        SalonProfile profile = TestSalonProfileFactory.standardForIT(prof, tenantId);
        salonProfileRepository.save(profile);

        Client client = TestClientFactory.standardForIt(tenantId);
        clientRepository.save(client);

        SalonService service = TestSalonServiceFactory.standardForIt(tenantId);
        salonServiceRepository.save(service);

        Appointment appointment = TestAppointmentFactory.standardForIt(client, prof, service);
        appointmentRepository.save(appointment);

        when(whatsappProvider.sendText(eq(tenantId), anyString(), eq(client.getPhoneNumber())))
                .thenThrow(new RuntimeException("Connection failed"));

        appointmentMessagingUseCase.sendAppointmentConfirmationMessage(appointment.getId());

        List<WhatsappMessage> messages = whatsappMessageRepository.findAll();
        assertThat(messages).hasSize(1);
        WhatsappMessage msg = messages.get(0);
        assertThat(msg.getMessageStatus()).isEqualTo(WhatsappMessageStatus.FAILED);
        assertThat(msg.getLastErrorMessage()).contains("Connection failed");
        assertThat(msg.getAttempts()).isEqualTo(1);
    }
}