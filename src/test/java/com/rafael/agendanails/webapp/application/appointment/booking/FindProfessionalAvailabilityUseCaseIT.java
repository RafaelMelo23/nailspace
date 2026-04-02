package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.FindProfessionalAvailabilityDTO;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.TestClockConfig;
import com.rafael.agendanails.webapp.support.factory.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest
@ActiveProfiles("it")
@Import(TestClockConfig.class)
class FindProfessionalAvailabilityUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private FindProfessionalAvailabilityUseCase findProfessionalAvailabilityUseCase;

    @Autowired
    private Clock clock;

    private record SetupData(
            Professional professional,
            SalonProfile salonProfile,
            Client client,
            UserPrincipal principal,
            String tenantId
    ) {
    }

    private SetupData setupStandard(String tenantId) {
        return setup(tenantId, 0);
    }

    private SetupData setup(String tenantId, int bufferMinutes) {
        return setup(tenantId, TestSalonProfileFactory.standardForIT(null, tenantId, bufferMinutes), null);
    }

    private SetupData setup(String tenantId, int bufferMinutes, List<WorkSchedule> customSchedules) {
        return setup(tenantId, TestSalonProfileFactory.standardForIT(null, tenantId, bufferMinutes), customSchedules);
    }

    private SetupData setup(String tenantId, SalonProfile salonProfile, List<WorkSchedule> customSchedules) {
        Professional professional = professionalRepository.save(TestProfessionalFactory.standardForIt(tenantId));
        salonProfile.setOwner(professional);
        salonProfile.setTenantId(tenantId);
        SalonProfile savedSalonProfile = salonProfileRepository.save(salonProfile);

        List<WorkSchedule> schedules = customSchedules != null ? customSchedules : TestWorkScheduleFactory.fullWeekForIt(professional);
        schedules.forEach(s -> {
            s.setProfessional(professional);
            s.setTenantId(tenantId);
        });
        workScheduleRepository.saveAll(schedules);
        professional.setWorkSchedules(new HashSet<>(schedules));

        Client client = clientRepository.save(TestClientFactory.standardForIt(tenantId));
        UserPrincipal principal = UserPrincipal.builder()
                .userId(client.getId())
                .tenantId(tenantId)
                .userRole(List.of(client.getUserRole()))
                .email(client.getEmail())
                .build();

        return new SetupData(professional, savedSalonProfile, client, principal, tenantId);
    }

    @Test
    void findAvailableTimes_shouldWorkForAnonymousUsers() {
        String tenantId = "tenant-test";
        SetupData setup = setupStandard(tenantId);
        Professional professional = setup.professional();

        SalonService service = salonServiceRepository.save(
                TestSalonServiceFactory.standardForIt(tenantId, 3600, null)
        );

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, null);

        assertThat(result.appointmentTimesDTOList()).isNotEmpty();
        assertThat(result.earliestRecommendedDate()).isNull();
    }

    @Test
    void findAvailableTimes_shouldFindAvailability_WhenAllFieldsAreValid() {
        String tenantId = "tenant-test";
        LocalDate frozenDate = LocalDate.now(clock);
        SetupData setup = setupStandard(tenantId);

        Client otherClient = clientRepository.save(TestClientFactory.standardForIt(tenantId));
        Professional professional = setup.professional();
        ZoneId zone = setup.salonProfile().getZoneId();

        SalonService salonService1 = salonServiceRepository.save(
                TestSalonServiceFactory.standardForIt(tenantId, 3600, null)
        );

        List<LocalTime[]> busyTimes = List.of(
                new LocalTime[]{LocalTime.of(9, 0), LocalTime.of(10, 0)},
                new LocalTime[]{LocalTime.of(11, 0), LocalTime.of(12, 0)},
                new LocalTime[]{LocalTime.of(15, 0), LocalTime.of(16, 0)}
        );

        appointmentRepository.saveAll(
                TestAppointmentFactory.multipleForDay(frozenDate, zone, otherClient, professional, salonService1, busyTimes)
        );

        SalonService salonService2 = salonServiceRepository.save(
                TestSalonServiceFactory.standardForIt(tenantId, 3600, null)
        );

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(7200)
                .servicesIds(List.of(salonService1.getId(), salonService2.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        assertThat(result.appointmentTimesDTOList()).hasSize(7);

        AppointmentTimesDTO firstDay = result.appointmentTimesDTOList().getFirst();
        assertThat(firstDay.date()).isEqualTo(frozenDate);

        assertThat(firstDay.availableTimes())
                .as("Should find available slots considering busy times and duration")
                .containsExactly(LocalTime.of(13, 0), LocalTime.of(16, 0));
    }

    @Test
    void findAvailableTimes_shouldRespectLoyalClientPriority_WhenPrioritized() {
        String tenantId = "tenant-test";
        SetupData setup = setup(tenantId, TestSalonProfileFactory.standardForIT(null, tenantId, true, 5, 15), null);
        Professional professional = setup.professional();

        SalonService service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt(tenantId));

        appointmentRepository.saveAll(TestAppointmentFactory.finishedAppointmentsForIt(setup.client(), professional, service, 3));

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        assertThat(result.appointmentTimesDTOList()).hasSize(15);
    }

    @Test
    void findAvailableTimes_shouldRespectMaintenanceInterval_WhenPreviousAppointmentExists() {
        String tenantId = "tenant-test";
        LocalDate frozenDate = LocalDate.now(clock);
        SetupData setup = setupStandard(tenantId);
        Professional professional = setup.professional();
        SalonProfile salonProfile = setup.salonProfile();

        SalonService service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt(
                tenantId, 3600, 15));

        Client client = setup.client();

        Instant tenDaysAgo = frozenDate.minusDays(10).atStartOfDay(salonProfile.getZoneId()).toInstant();
        appointmentRepository.save(TestAppointmentFactory.atSpecificTimeForIt(
                tenDaysAgo, tenDaysAgo.plus(1, java.time.temporal.ChronoUnit.HOURS),
                client, professional, service, AppointmentStatus.FINISHED
        ));

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        LocalDate expectedStart = frozenDate.plusDays(2);
        assertThat(result.appointmentTimesDTOList().getFirst().date()).isEqualTo(expectedStart);
    }

    @Test
    void findAvailableTimes_shouldApplySalonBuffer_WhenCalculatingSlots() {
        String tenantId = "tenant-test";
        LocalDate frozenDate = LocalDate.now(clock);

        List<WorkSchedule> schedules = TestWorkScheduleFactory.fullWeekForIt(
                null,
                LocalTime.of(9, 0),
                LocalTime.of(19, 0),
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        );
        SetupData setup = setup(tenantId, 30, schedules);
        Professional professional = setup.professional();
        SalonProfile salonProfile = setup.salonProfile();

        SalonService service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt(tenantId, 3600, null));
        Client otherClient = clientRepository.save(TestClientFactory.standardForIt(tenantId));

        appointmentRepository.save(TestAppointmentFactory.atLocalTimeForIt(
                frozenDate, LocalTime.of(10, 0), LocalTime.of(11, 0),
                salonProfile.getZoneId(), otherClient, professional, service, AppointmentStatus.CONFIRMED
        ));

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        AppointmentTimesDTO day = result.appointmentTimesDTOList().getFirst();

        assertThat(day.availableTimes())
                .contains(LocalTime.of(9, 0), LocalTime.of(11, 30))
                .doesNotContain(LocalTime.of(11, 0));
    }

    @Test
    void findAvailableTimes_shouldHandleProfessionalScheduleBlocks() {
        String tenantId = "tenant-test";
        LocalDate frozenDate = LocalDate.now(clock);
        SetupData setup = setupStandard(tenantId);
        Professional professional = setup.professional();
        SalonProfile salonProfile = setup.salonProfile();

        SalonService service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt(tenantId, 3600, null));

        Instant blockStart = frozenDate.atTime(LocalTime.of(14, 0)).atZone(salonProfile.getZoneId()).toInstant();
        Instant blockEnd = frozenDate.atTime(LocalTime.of(17, 0)).atZone(salonProfile.getZoneId()).toInstant();

        scheduleBlockRepository.save(ScheduleBlock.builder()
                .professional(professional)
                .dateStartTime(blockStart)
                .dateEndTime(blockEnd)
                .reason("Doctor")
                .tenantId(tenantId)
                .build());

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        AppointmentTimesDTO day = result.appointmentTimesDTOList().getFirst();
        assertThat(day.availableTimes())
                .contains(LocalTime.of(13, 0), LocalTime.of(17, 0))
                .doesNotContain(LocalTime.of(14, 0), LocalTime.of(15, 0), LocalTime.of(16, 0));
    }

    @Test
    void findAvailableTimes_shouldHandleEarliestRecommendedDate() {
        String tenantId = "tenant-test";

        SetupData setup = setup(tenantId, 0);
        Client client = setup.client();
        Professional professional = setup.professional();
        SalonService service = salonServiceRepository.save(
                TestSalonServiceFactory.standardForIt(tenantId, 3600, 15)
        );

        Instant now = clock.instant();
        Instant start = now.minus(5, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        Appointment previousAppointment = TestAppointmentFactory.atSpecificTimeForIt(
                start,
                end,
                client,
                professional,
                service,
                AppointmentStatus.FINISHED);
        previousAppointment.setTenantId(tenantId);
        appointmentRepository.save(previousAppointment);

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        ZonedDateTime expectedResult = previousAppointment.getEndDate()
                .plus(service.getMaintenanceIntervalDays(), ChronoUnit.DAYS)
                .atZone(setup.salonProfile().getZoneId());

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        assertThat(result.earliestRecommendedDate())
                .isEqualTo(expectedResult);
    }

    @Test
    void findAvailableTimes_shouldReturnNow_whenRecommendedDateIsInThePast() {
        String tenantId = "tenant-test";

        SetupData setup = setup(tenantId, 0);
        Client client = setup.client();
        Professional professional = setup.professional();
        SalonService service = salonServiceRepository.save(
                TestSalonServiceFactory.standardForIt(tenantId, 3600, 15)
        );

        Instant now = clock.instant();
        Instant start = now.minus(18, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        Appointment previousAppointment = TestAppointmentFactory.atSpecificTimeForIt(
                start,
                end,
                client,
                professional,
                service,
                AppointmentStatus.FINISHED);
        previousAppointment.setTenantId(tenantId);
        appointmentRepository.save(previousAppointment);

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professional.getExternalId().toString())
                .serviceDurationInSeconds(3600)
                .servicesIds(List.of(service.getId()))
                .build();

        ZonedDateTime expectedResult = now.atZone(setup.salonProfile().getZoneId());

        TenantContext.setTenant(setup.tenantId());
        var result = findProfessionalAvailabilityUseCase.findAvailableTimes(dto, setup.principal());

        assertThat(result.earliestRecommendedDate())
                .isEqualTo(expectedResult);
    }
}
