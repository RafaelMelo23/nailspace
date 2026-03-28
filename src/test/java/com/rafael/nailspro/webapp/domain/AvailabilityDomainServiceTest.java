package com.rafael.nailspro.webapp.domain;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import com.rafael.nailspro.webapp.domain.model.WorkSchedule;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.domain.repository.ScheduleBlockRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.nailspro.webapp.domain.model.TimeInterval;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.support.factory.*;
import com.rafael.nailspro.webapp.support.factory.TestScheduleBlockFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityDomainServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private ProfessionalRepository professionalRepository;

    @InjectMocks
    private AvailabilityDomainService availabilityDomainService;

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static final LocalDate FRIDAY = LocalDate.of(2026, 3, 20);
    private static final LocalDate SATURDAY = FRIDAY.plusDays(1);

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

    private void mockAppointments(List<Appointment> appointments) {
        when(appointmentRepository.findBusyAppointmentsInRange(
                any(),
                any(),
                any(),
                eq(List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.FINISHED))
        )).thenReturn(appointments);
    }

    private void mockBlocks(List<ScheduleBlock> blocks) {
        when(scheduleBlockRepository.findBusyBlocksInRange(
                any(),
                any(),
                any()
        )).thenReturn(blocks);
    }

    @Test
    void checkIfProfessionalHasTimeConflicts_NoConflict_CompletesSilently() {
        UUID professionalId = UUID.randomUUID();
        TimeInterval interval = buildInterval();

        when(professionalRepository.hasTimeConflicts(
                eq(professionalId),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
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
                any(LocalDateTime.class),
                any(LocalDateTime.class),
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
    void getProfessionalBusyIntervals_EmptyRepositories_ReturnsEmptyList() {
        Professional professional = professionalWithFridaySchedule();

        LocalDate startRange = FRIDAY;
        LocalDate endRange = SATURDAY;

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockAppointments(List.of());
        mockBlocks(List.of());

        List<BusyInterval> result =
                availabilityDomainService.getProfessionalBusyIntervals(
                        professional,
                        startRange,
                        endRange,
                        salonProfile
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void findAvailableTimes_CompletelyFreeDay_ReturnsAllSlotsGenerated() {

        Professional professional = professionalWithFridaySchedule();

        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockAppointments(List.of());
        mockBlocks(List.of());

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(
                        professional,
                        window,
                        salonProfile,
                        3600
                );

        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(day -> day.availableTimes().contains(LocalTime.of(12, 0))));
    }

    @Test
    void findAvailableTimes_WithOverlappingAppointmentsAndBlocks_CalculatesCorrectGaps() {
        Professional professional = professionalWithFridaySchedule();
        LocalDate date = FRIDAY;
        AppointmentTimeWindow window = windowFor(date);

        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockAppointments(List.of(
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 10, 0),
                        atTime(date, 11, 0),
                        professional,
                        AppointmentStatus.CONFIRMED
                ),
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 13, 0),
                        atTime(date, 14, 0),
                        professional,
                        AppointmentStatus.CONFIRMED
                )
        ));

        mockBlocks(List.of(
                TestScheduleBlockFactory.atSpecificTime(
                        atTime(date, 15, 0),
                        atTime(date, 16, 0),
                        professional
                )
        ));

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(
                        professional,
                        window,
                        salonProfile,
                        3600
                );

        assertFalse(result.isEmpty());

        List<LocalTime> expected = List.of(
                LocalTime.of(9, 0),
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

        mockAppointments(List.of(
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 10, 0),
                        atTime(date, 11, 0),
                        professional,
                        AppointmentStatus.CONFIRMED
                ),
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 12, 0),
                        atTime(date, 13, 0),
                        professional,
                        AppointmentStatus.CONFIRMED
                )
        ));

        mockBlocks(List.of());

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(
                        professional,
                        window,
                        salonProfile,
                        3600
                );

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

        mockAppointments(List.of(
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 9, 30),
                        atTime(date, 9, 50),
                        professional,
                        AppointmentStatus.CONFIRMED
                )
        ));

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(
                        professional,
                        window,
                        salonProfile,
                        3600
                );

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

        mockAppointments(List.of(
                TestAppointmentFactory.atSpecificTime(
                        atTime(date, 10, 0),
                        atTime(date, 10, 45),
                        professional,
                        AppointmentStatus.FINISHED
                )
        ));

        List<AppointmentTimesDTO> result =
                availabilityDomainService.findAvailableTimes(
                        professional,
                        window,
                        salonProfile,
                        3600
                );

        System.out.println(result);

        assertNotNull(result);

    }
}