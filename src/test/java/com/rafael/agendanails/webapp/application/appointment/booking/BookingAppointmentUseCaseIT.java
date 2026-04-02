package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("it")
class BookingAppointmentUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private BookingAppointmentUseCase bookingAppointmentUseCase;

    private Client client;
    private UserPrincipal principal;
    private final String tenantId = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(tenantId);

        this.client = clientRepository.save(
                TestClientFactory.standardForIt()
        );
        this.principal = UserPrincipal.builder()
                .userId(client.getId())
                .email(client.getEmail())
                .tenantId(tenantId)
                .userRole(List.of(UserRole.CLIENT))
                .build();
    }

    private PreparationData prepareData(
            int bufferMinutes,
            int mainServiceDurationSeconds,
            List<Integer> addOnDurationsSeconds
    ) {
        Professional professional = professionalRepository.save(
                TestProfessionalFactory.standardForIt()
        );

        List<WorkSchedule> schedules = TestWorkScheduleFactory.fullWeekForIt(professional);
        workScheduleRepository.saveAll(schedules);

        SalonProfile salonProfile = salonProfileRepository.save(
                TestSalonProfileFactory.standardForIT(professional, tenantId, bufferMinutes)
        );

        SalonService mainService = TestSalonServiceFactory.standardForIt(tenantId);
        mainService.setDurationInSeconds(mainServiceDurationSeconds);
        mainService.setId(null);
        mainService = salonServiceRepository.save(mainService);

        List<SalonService> addOns = addOnDurationsSeconds.stream()
                .map(duration -> {
                    SalonService addOn = TestSalonServiceFactory.addOnWithoutMaintenanceInterval();
                    addOn.setId(null);
                    addOn.setTenantId(tenantId);
                    addOn.setDurationInSeconds(duration);
                    return salonServiceRepository.save(addOn);
                })
                .toList();

        return new PreparationData(professional, salonProfile, mainService, addOns);
    }

    private record PreparationData(
            Professional professional,
            SalonProfile salonProfile,
            SalonService mainService,
            List<SalonService> addOns
    ) {
    }

    @Test
    void bookAppointment_shouldSuccessfullyBookWhenEverythingIsValid() {
        PreparationData data = prepareData(15, 3600, List.of(900));

        ZonedDateTime appointmentTime = ZonedDateTime.of(
                2026, 4, 6, 10, 0, 0, 0,
                ZoneId.of("America/Sao_Paulo")
        );

        AppointmentCreateDTO dto = AppointmentCreateDTO.builder()
                .professionalExternalId(data.professional().getExternalId().toString())
                .mainServiceId(data.mainService().getId())
                .addOnsIds(data.addOns().stream().map(SalonService::getId).toList())
                .zonedAppointmentDateTime(appointmentTime)
                .observation("Testing booking")
                .build();

        bookingAppointmentUseCase.bookAppointment(dto, principal);

        List<Appointment> appointments = appointmentRepository.findAll();
        assertThat(appointments).hasSize(1);

        Appointment appointment = appointments.getFirst();
        assertThat(appointment.getProfessional().getId()).isEqualTo(data.professional().getId());
        assertThat(appointment.getMainSalonService().getId()).isEqualTo(data.mainService().getId());
        assertThat(appointment.getClient().getId()).isEqualTo(client.getId());
    }

    @Test
    void bookAppointment_shouldRollback_whenProfessionalIsBusy() {
        PreparationData data = prepareData(15, 3600, List.of(900));

        ZonedDateTime appointmentTime = ZonedDateTime.of(
                2026, 4, 6, 10, 0, 0, 0,
                ZoneId.of("America/Sao_Paulo")
        );

        var clientB = clientRepository.save(
                TestClientFactory.standardForIt()
        );

        var alreadySavedAppointment = appointmentRepository.save(TestAppointmentFactory.atSpecificTimeForIt(
                appointmentTime.toInstant(),
                appointmentTime.plusHours(1).toInstant(),
                clientB,
                data.professional(),
                data.mainService,
                AppointmentStatus.CONFIRMED
        ));

        AppointmentCreateDTO dto = AppointmentCreateDTO.builder()
                .professionalExternalId(data.professional().getExternalId().toString())
                .mainServiceId(data.mainService().getId())
                .addOnsIds(data.addOns().stream().map(SalonService::getId).toList())
                .zonedAppointmentDateTime(appointmentTime)
                .observation("Testing booking")
                .build();

        assertThrows(BusinessException.class,
                () ->bookingAppointmentUseCase.bookAppointment(dto, principal));

        List<Appointment> allSavedAppointments = appointmentRepository.findAll();

        assertThat(allSavedAppointments)
                .hasSize(1)
                .extracting(Appointment::getClient)
                .containsExactly(clientB);
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void bookAppointment_shouldSucceedForBoth_whenConcurrentBookingOnDifferentSlots() throws InterruptedException {
        PreparationData data = prepareData(15, 3600, List.of());

        ZonedDateTime time1 = ZonedDateTime.of(2026, 4, 6, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));
        ZonedDateTime time2 = ZonedDateTime.of(2026, 4, 6, 11, 30, 0, 0, ZoneId.of("America/Sao_Paulo"));

        Client client2 = clientRepository.save(TestClientFactory.standardForIt());
        UserPrincipal principal2 = UserPrincipal.builder()
                .userId(client2.getId())
                .email(client2.getEmail())
                .tenantId(tenantId)
                .userRole(List.of(UserRole.CLIENT))
                .build();

        AppointmentCreateDTO dto1 = AppointmentCreateDTO.builder()
                .professionalExternalId(data.professional().getExternalId().toString())
                .mainServiceId(data.mainService().getId())
                .addOnsIds(List.of())
                .zonedAppointmentDateTime(time1)
                .observation("Booking 1")
                .build();

        AppointmentCreateDTO dto2 = AppointmentCreateDTO.builder()
                .professionalExternalId(data.professional().getExternalId().toString())
                .mainServiceId(data.mainService().getId())
                .addOnsIds(List.of())
                .zonedAppointmentDateTime(time2)
                .observation("Booking 2")
                .build();

        int threadCount = 2;
        AtomicInteger successCount;
        AtomicInteger failureCount;
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            successCount = new AtomicInteger(0);
            failureCount = new AtomicInteger(0);

            executorService.submit(() -> {
                try {
                    TenantContext.setTenant(tenantId);
                    startLatch.await();
                    bookingAppointmentUseCase.bookAppointment(dto1, principal);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            executorService.submit(() -> {
                try {
                    TenantContext.setTenant(tenantId);
                    startLatch.await();
                    bookingAppointmentUseCase.bookAppointment(dto2, principal2);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            doneLatch.await();
            executorService.shutdown();
        }

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failureCount.get()).isEqualTo(0);

        List<Appointment> appointments = appointmentRepository.findAll();

        long count = appointments.stream()
                .filter(a -> a.getProfessional().getId().equals(data.professional().getId()))
                .count();
        assertThat(count).isEqualTo(2);
    }
}