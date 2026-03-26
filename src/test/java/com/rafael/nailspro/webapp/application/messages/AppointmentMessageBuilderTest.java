package com.rafael.nailspro.webapp.application.messages;

import com.rafael.nailspro.webapp.domain.model.*;
import com.rafael.nailspro.webapp.infrastructure.helper.DateAndZoneHelper;
import com.rafael.nailspro.webapp.infrastructure.helper.TenantUrlProvider;
import com.rafael.nailspro.webapp.support.factory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentMessageBuilderTest {

    @Mock
    private TenantUrlProvider urlProvider;
    @Mock
    private DateAndZoneHelper dateHelper;

    @InjectMocks
    private AppointmentMessageBuilder appointmentMessageBuilder;

    @Test
    void buildAppointmentConfirmationMessage_returnsCompleteMessage_whenValidAppointmentWithAddOns() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish(); // Full Name: Jane Doe
        SalonService mainService = TestSalonServiceFactory.manicure(); // Name: Manicure, Value: 50 (from factory builder)
        SalonService addonService = TestSalonServiceFactory.pedicure(); // Name: Pedicure, Value: 50
        AppointmentAddOn addon = TestAppointmentAddOnFactory.standardEnglish(addonService);
        
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, mainService, List.of(addon));
        String tenantId = appointment.getTenantId();
        Long appointmentId = appointment.getId();

        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
        when(dateHelper.toZonedDateTime(appointment.getStartDate(), tenantId)).thenReturn(zonedDateTime);
        when(urlProvider.buildCancelAppointmentUrl(tenantId, appointmentId)).thenReturn("https://cancel.com/123");

        String result = appointmentMessageBuilder.buildAppointmentConfirmationMessage(appointment);

        assertTrue(result.contains("Olá, Jane! ✨"));
        assertTrue(result.contains("Sua reserva na Beauty Salon está confirmada!"));
        assertTrue(result.contains("📅 Data: 10/04 às 10:00"));
        assertTrue(result.contains("👤 Profissional: John Professional"));
        assertTrue(result.contains("💅 Serviços: Manicure, Pedicure"));
        assertTrue(result.contains("- Principal: 50"));
        assertTrue(result.contains("- Extras: 50"));
        assertTrue(result.contains("- Total: 100"));
        assertTrue(result.contains("https://cancel.com/123"));
    }

    @Test
    void buildAppointmentConfirmationMessage_returnsMessageWithoutExtras_whenNoAddOns() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.builder().fullName("Maria Silva").tenantId("tenant-123").build();
        SalonService mainService = TestSalonServiceFactory.manicure();
        
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, mainService, List.of());
        String tenantId = appointment.getTenantId();

        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 4, 15, 14, 30, 0, 0, ZoneId.of("America/Sao_Paulo"));
        when(dateHelper.toZonedDateTime(appointment.getStartDate(), tenantId)).thenReturn(zonedDateTime);
        when(urlProvider.buildCancelAppointmentUrl(tenantId, appointment.getId())).thenReturn("https://cancel.com/456");

        String result = appointmentMessageBuilder.buildAppointmentConfirmationMessage(appointment);

        assertTrue(result.contains("Olá, Maria! ✨"));
        assertTrue(result.contains("💅 Serviços: Manicure"));
        assertTrue(result.contains("- Principal: 50"));
        assertTrue(result.contains("- Extras: 0"));
        assertTrue(result.contains("- Total: 50"));
    }

    @Test
    void buildAppointmentReminderMessage_returnsShortMessage_whenValidAppointment() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish(); // Jane Doe
        SalonService mainService = TestSalonServiceFactory.manicure();
        
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, mainService, List.of());
        
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
        when(dateHelper.toZonedDateTime(appointment.getStartDate())).thenReturn(zonedDateTime);
        when(urlProvider.buildCancelAppointmentUrl(appointment.getTenantId(), appointment.getId())).thenReturn("https://cancel.com/123");

        String result = appointmentMessageBuilder.buildAppointmentReminderMessage(appointment);

        assertTrue(result.contains("Olá, Jane! 🕒"));
        assertTrue(result.contains("Passando para lembrar do seu momento hoje na Beauty Salon."));
        assertTrue(result.contains("⏰ Horário: 10:00"));
        assertTrue(result.contains("👤 Profissional: John Professional"));
        assertTrue(result.contains("https://cancel.com/123"));
    }

    @Test
    void buildAppointmentConfirmationMessage_handlesLongNames_whenExtremelyLongNamesProvided() {
        String longClientName = "Maria".repeat(20) + " Silva";
        Client client = TestClientFactory.builder().fullName(longClientName).tenantId("tenant-123").build();
        Professional professional = TestProfessionalFactory.builder().fullName("Prof".repeat(20)).tenantId("tenant-123").build();
        SalonService service = TestSalonServiceFactory.manicure();

        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());
        
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
        when(dateHelper.toZonedDateTime(appointment.getStartDate(), "tenant-123")).thenReturn(zonedDateTime);

        String result = appointmentMessageBuilder.buildAppointmentConfirmationMessage(appointment);

        assertTrue(result.contains("Olá, " + "Maria".repeat(20) + "! ✨"));
        assertTrue(result.contains("👤 Profissional: " + "Prof".repeat(20)));
    }

    @Test
    void buildAppointmentConfirmationMessage_usesDefaultGreeting_whenClientNameIsEmpty() {
        Client client = TestClientFactory.builder().fullName("").tenantId("tenant-123").build();
        Professional professional = TestProfessionalFactory.standardEnglish();
        SalonService service = TestSalonServiceFactory.manicure();

        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());
        
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2026, 4, 10, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
        when(dateHelper.toZonedDateTime(appointment.getStartDate(), "tenant-123")).thenReturn(zonedDateTime);

        String result = appointmentMessageBuilder.buildAppointmentConfirmationMessage(appointment);

        assertTrue(result.contains("Olá, cliente! ✨"));
    }
}