package com.rafael.agendanails.webapp.domain;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.config.CacheConfig;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.date.SimpleBusyInterval;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.support.factory.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityDomainServiceTest {

    @Mock
    private ProfessionalRepository professionalRepository;

    @Mock
    private BusyIntervalService busyIntervalService;

    @InjectMocks
    private AvailabilityDomainService availabilityDomainService;

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final LocalDate FRIDAY = LocalDate.of(2026, 3, 20);

    private Cache cache;
    private CacheManager cacheManager;


    private Professional professionalWithFridaySchedule() {
        Professional professional = TestProfessionalFactory.standard();
        WorkSchedule schedule = TestWorkScheduleFactory.standard(DayOfWeek.FRIDAY, professional);
        return professionalWithSchedule(schedule);
    }

    private Professional professionalWithSchedule(WorkSchedule schedule) {
        return TestProfessionalFactory.withSchedules(Set.of(schedule));
    }

    private static TimeInterval buildInterval() {
        return buildInterval(2026, 3, 20, 10, 0, 60, 15);
    }

    private static @NotNull TimeInterval buildInterval(
            int year,
            int month,
            int day,
            int hour,
            int minute,
            int durationMinutes,
            int bufferMinutes
    ) {
        Instant start = LocalDateTime.of(year, month, day, hour, minute)
                .atZone(ZONE)
                .toInstant();

        Instant end = start.plus(durationMinutes, ChronoUnit.MINUTES);
        Instant endWithBuffer = end.plus(bufferMinutes, ChronoUnit.MINUTES);

        return new TimeInterval(start, end, endWithBuffer, ZONE);
    }

    private static AppointmentTimeWindow windowFor(LocalDate date) {
        return new AppointmentTimeWindow(date, date.plusDays(1));
    }

    private static Instant atTime(LocalDate date, int hour, int minute) {
        return LocalDateTime.of(date, LocalTime.of(hour, minute))
                .atZone(ZONE)
                .toInstant();
    }

    private void mockBusyIntervals(LocalDate date, List<BusyInterval> intervals) {
        when(busyIntervalService.getIntervalsWithCaching(any())).thenReturn(Map.of(date, intervals));
    }

    @Test
    void checkIfProfessionalHasTimeConflicts_NoConflict_CompletesSilently() {
        UUID professionalId = UUID.randomUUID();
        TimeInterval interval = buildInterval();

        when(professionalRepository.hasTimeConflicts(
                eq(professionalId),
                any(Instant.class),
                any(Instant.class),
                eq(List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.FINISHED))
        )).thenReturn(false);

        assertDoesNotThrow(() ->
                availabilityDomainService.checkIfProfessionalHasTimeConflicts(professionalId, interval)
        );
    }

    @Test
    void checkIfProfessionalHasTimeConflicts_WithConflict_ThrowsBusinessException() {
        UUID professionalId = UUID.randomUUID();
        TimeInterval interval = buildInterval();

        when(professionalRepository.hasTimeConflicts(
                eq(professionalId),
                any(Instant.class),
                any(Instant.class),
                eq(List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.FINISHED))
        )).thenReturn(true);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> availabilityDomainService.checkIfProfessionalHasTimeConflicts(professionalId, interval)
        );

        assertEquals(
                "O profissional já possui um compromisso ou bloqueio neste horário.",
                exception.getMessage()
        );
    }

    @Test
    void findAvailableTimes_CompletelyFreeDay_ReturnsAllSlotsGenerated() {

        Professional professional = professionalWithFridaySchedule();

        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockBusyIntervals(date, List.of());

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(3600)
                .build();

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(query);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(day -> day.availableTimes().contains(LocalTime.of(12, 0))));
    }

    @Test
    void findAvailableTimes_WithOverlappingAppointmentsAndBlocks_CalculatesCorrectGaps() {
        Professional professional = professionalWithFridaySchedule();
        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);
        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        List<BusyInterval> busy = new ArrayList<>();
        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(10, 0))
                .end(LocalTime.of(11, 0))
                .date(date)
                .build());

        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(13, 0))
                .end(LocalTime.of(14, 0))
                .date(date)
                .build());
        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(15, 0))
                .end(LocalTime.of(16, 0))
                .date(date)
                .build());

        mockBusyIntervals(date, busy);

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(3600)
                .build();

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(query);

        assertFalse(result.isEmpty());

        List<LocalTime> expected = List.of(
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                LocalTime.of(16, 30),
                LocalTime.of(17, 0)
        );

        assertEquals(expected, result.getFirst().availableTimes());
    }

    @Test
    void findAvailableTimes_ExactGapFit_ReturnsSingleSlot() {

        Professional professional = TestProfessionalFactory.standard();
        WorkSchedule schedule = TestWorkScheduleFactory.custom(
                DayOfWeek.FRIDAY,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null,
                null,
                professional
        );
        professional = professionalWithSchedule(schedule);

        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.withCustomBuffer(0);

        List<BusyInterval> busy = new ArrayList<>();
        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(10, 0))
                .end(LocalTime.of(11, 0))
                .date(date)
                .build());
        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(12, 0))
                .end(LocalTime.of(13, 0))
                .date(date)
                .build());

        mockBusyIntervals(date, busy);

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(3600)
                .build();

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(query);

        assertEquals(1, result.size());
        assertEquals(List.of(LocalTime.of(11, 0)), result.getFirst().availableTimes());
    }

    @Test
    void findAvailableTimes_GapTooSmall_ReturnsNoSlotsForGap() {
        Professional professional = TestProfessionalFactory.standard();
        WorkSchedule workSchedule = TestWorkScheduleFactory.custom(
                DayOfWeek.FRIDAY,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null,
                null,
                professional
        );
        professional = professionalWithSchedule(workSchedule);

        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        List<BusyInterval> busy = new ArrayList<>();
        busy.add(SimpleBusyInterval.builder()
                .start(LocalTime.of(9, 30))
                .end(LocalTime.of(9, 50))
                .date(date)
                .build());

        mockBusyIntervals(date, busy);

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(3600)
                .build();

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(query);

        assertNotNull(result);
        assertTrue(result.getFirst().availableTimes().isEmpty());
    }

    @Test
    void findAvailableTimes_NonConfirmedAppointments_ReturnsFreeSlot() {
        Professional professional = TestProfessionalFactory.standard();
        WorkSchedule workSchedule = TestWorkScheduleFactory.custom(
                DayOfWeek.FRIDAY,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null,
                null,
                professional
        );
        professional = professionalWithSchedule(workSchedule);

        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        // The logic for filtering CONFIRMED/FINISHED is now in BusyIntervalService.
        // In this test, we mock that service, so we decide what it returns.
        mockBusyIntervals(date, List.of());

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .serviceDurationInSeconds(3600)
                .build();

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(query);

        assertNotNull(result);
        assertFalse(result.getFirst().availableTimes().isEmpty());
    }
}