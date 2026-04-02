package com.rafael.agendanails.webapp.application.appointment;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.TestClockConfig;
import com.rafael.agendanails.webapp.support.factory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("it")
@Import(TestClockConfig.class)
class BookingPolicyServiceIT extends BaseIntegrationTest {

    @Autowired
    private BookingPolicyService bookingPolicyService;

    @Autowired
    private java.time.Clock clock;

    @Test
    void enforceBookingHorizon_shouldValidateSuccessfully_whenDataIsValid() {
        TenantContext.setTenant("tenant-test");
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, "tenant-test"));

        var client = clientRepository.save(TestClientFactory.standardForIt());
        var principal = UserPrincipal.builder()
                .userId(client.getId())
                .userRole(client.getEffectiveRoles())
                .email(client.getEmail())
                .tenantId(client.getTenantId())
                .build();

        var bookingDate = Instant.now(clock).plus(3, ChronoUnit.DAYS);
        assertDoesNotThrow(
                () -> bookingPolicyService.enforceBookingHorizon(
                LocalDateTime.ofInstant(bookingDate, ZoneId.of("America/Sao_Paulo")),
                principal));
    }

    @Test
    void enforceBookingHorizon_shouldNotAllowTenantMismatch() {
        TenantContext.setTenant("random-tenant");
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt("random-tenant"));
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, "random-tenant"));

        TenantContext.setTenant("tenant-test");
        var client = clientRepository.save(TestClientFactory.standardForIt("tenant-test"));
        var principal = UserPrincipal.builder()
                .userId(client.getId())
                .userRole(client.getEffectiveRoles())
                .email(client.getEmail())
                .tenantId(client.getTenantId())
                .build();

        var bookingDate = Instant.now(clock).plus(3, ChronoUnit.DAYS);
        assertThrows(BusinessException.class,
                () -> bookingPolicyService.enforceBookingHorizon(
                        LocalDateTime.ofInstant(bookingDate, ZoneId.of("America/Sao_Paulo")),
                        principal));
    }

    @Test
    void calculateAllowedWindow_shouldValidateSuccessfully_whenDataIsValid() {
        TenantContext.setTenant("tenant-test");
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional));

        var client = clientRepository.save(TestClientFactory.standardForIt());
        var principal = UserPrincipal.builder()
                .userId(client.getId())
                .userRole(client.getEffectiveRoles())
                .email(client.getEmail())
                .tenantId(client.getTenantId())
                .build();

        var service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt());

        var app = appointmentRepository.save(TestAppointmentFactory.atSpecificTimeForIt(
                Instant.now(clock).minus(10, ChronoUnit.DAYS),
                Instant.now(clock).minus(10, ChronoUnit.DAYS),
                client,
                professional,
                service,
                AppointmentStatus.FINISHED));

        AppointmentTimeWindow result = bookingPolicyService.calculateAllowedWindow(List.of(service), principal.getTenantId(), principal.getUserId());

        LocalDate expectedStart = LocalDate.ofInstant(
                app.getEndDate().plus(service.getMaintenanceIntervalDays() - 3, ChronoUnit.DAYS),
                ZoneId.of("America/Sao_Paulo"));

        assertThat(result.start())
                .isEqualTo(expectedStart);

        assertThat(result.end())
                .isEqualTo(expectedStart.plusDays(7));
    }
}