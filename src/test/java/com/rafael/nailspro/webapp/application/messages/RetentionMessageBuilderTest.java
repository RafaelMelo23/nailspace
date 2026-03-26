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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionMessageBuilderTest {

    @Mock
    private TenantUrlProvider urlProvider;

    @InjectMocks
    private RetentionMessageBuilder retentionMessageBuilder;

    @Test
    void shouldBuildMessageForMultipleServices() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        SalonService service1 = TestSalonServiceFactory.manicure();
        SalonService service2 = TestSalonServiceFactory.pedicure();
        AppointmentAddOn addon = TestAppointmentAddOnFactory.standardEnglish(service2);
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service1, List.of(addon));

        Instant returnInstant = Instant.parse("2026-04-10T10:00:00Z");

        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional,
                client,
                List.of(service1, service2),
                appointment,
                returnInstant
        );

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        String expected = """
                Olá, Jane Doe!
                Notamos que a data de manutenção dos seus serviços está se aproximando.
                📅 Sugestão: 10/04
                💅 Serviços: Manicure, Pedicure
                Como nossa agenda costuma lotar rápido,
                liberei o link para você garantir sua vaga com antecedência: https://agenda.com/123""";

        assertEquals(expected, result);
    }

    @Test
    void shouldBuildMessageForSingleService() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        SalonService service = TestSalonServiceFactory.manicure();
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());

        Instant returnInstant = Instant.parse("2026-04-15T14:00:00Z");

        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(professional, client, List.of(service), appointment, returnInstant);

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        String expected = """
                Olá, Jane Doe!
                Notamos que a data de manutenção do seu serviço está se aproximando.
                📅 Sugestão: 15/04
                💅 Serviço: Manicure
                Como nossa agenda costuma lotar rápido,
                liberei o link para você garantir sua vaga com antecedência: https://agenda.com/123""";

        assertEquals(expected, result);
    }

    @Test
    void shouldBuildMessageForThreeServices() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        SalonService s1 = TestSalonServiceFactory.manicure();
        SalonService s2 = TestSalonServiceFactory.pedicure();
        SalonService s3 = TestSalonServiceFactory.builder().name("Nail Art").tenantId("tenant-123").build();
        
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, s1, List.of());

        Instant returnInstant = Instant.parse("2026-05-20T10:00:00Z");

        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional, client, List.of(s1, s2, s3), appointment, returnInstant);

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        assertTrue(result.contains("dos seus serviços"));
        assertTrue(result.contains("Serviços: Manicure, Pedicure, Nail Art"));
        assertTrue(result.contains("20/05"));
    }

    @Test
    void shouldAdjustDateWhenTimeZoneCrossesMidnight() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        SalonService service = TestSalonServiceFactory.manicure();

        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());
        Instant returnInstant = Instant.parse("2026-04-10T01:00:00Z"); 

        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional, client, List.of(service), appointment, returnInstant);

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        assertTrue(result.contains("Sugestão: 09/04"));
    }

    @Test
    void shouldHandleLeapYear() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        SalonService service = TestSalonServiceFactory.manicure();
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());

        Instant returnInstant = Instant.parse("2028-02-29T12:00:00Z");

        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional, client, List.of(service), appointment, returnInstant);

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        assertTrue(result.contains("Sugestão: 29/02"));
    }

    @Test
    void buildRetentionMessage_returnsMessageWithEmptyServices_whenServiceListIsEmpty() {
        Professional professional = TestProfessionalFactory.standardEnglish();
        Client client = TestClientFactory.standardEnglish();
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, TestSalonServiceFactory.manicure(), List.of());
        
        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional, client, List.of(), appointment, Instant.now());

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        assertTrue(result.contains("manutenção do seu serviço"));
        assertTrue(result.contains("💅 Serviço: \n"));
    }

    @Test
    void shouldHandleVeryLongNames() {
        Professional professional = TestProfessionalFactory.builder()
                .fullName("A".repeat(100))
                .tenantId("tenant-123")
                .build();
        Client client = TestClientFactory.builder()
                .fullName("B".repeat(100))
                .tenantId("tenant-123")
                .build();
        SalonService service = TestSalonServiceFactory.builder()
                .name("C".repeat(100))
                .tenantId("tenant-123")
                .build();
        
        Appointment appointment = TestAppointmentFactory.standardEnglish(client, professional, service, List.of());
        RetentionForecast forecast = TestRetentionForecastFactory.standardEnglish(
                professional, client, List.of(service), appointment, Instant.now());

        when(urlProvider.buildBookAppointmentUrl("tenant-123")).thenReturn("https://agenda.com/123");

        String result = retentionMessageBuilder.buildRetentionMessage(forecast);

        assertTrue(result.contains("Olá, " + "B".repeat(100)));
        assertTrue(result.contains("💅 Serviço: " + "C".repeat(100)));
    }
}