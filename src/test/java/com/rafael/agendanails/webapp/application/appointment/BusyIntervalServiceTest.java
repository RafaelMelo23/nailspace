package com.rafael.agendanails.webapp.application.appointment;

import com.rafael.agendanails.webapp.domain.BusyIntervalService;
import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.domain.repository.ScheduleBlockRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonProfileFactory;
import com.rafael.agendanails.webapp.support.factory.TestWorkScheduleFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusyIntervalServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ScheduleBlockRepository scheduleBlockRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private BusyIntervalService busyIntervalService;

    private static final LocalDate FRIDAY = LocalDate.of(2026, 3, 20);

    @BeforeEach
    void setUp() {
        lenient().when(cacheManager.getCache(any())).thenReturn(cache);
    }

    private Professional professionalWithFridaySchedule() {
        Professional professional = TestProfessionalFactory.standard();
        WorkSchedule schedule = TestWorkScheduleFactory.standard(DayOfWeek.FRIDAY, professional);
        return TestProfessionalFactory.withSchedules(Set.of(schedule));
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
    void getProfessionalBusyIntervals_EmptyRepositories_ReturnsEmptyList() {
        Professional professional = professionalWithFridaySchedule();
        AppointmentTimeWindow window = new AppointmentTimeWindow(FRIDAY, FRIDAY.plusDays(1));
        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockAppointments(List.of());
        mockBlocks(List.of());

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .build();

        List<BusyInterval> result = busyIntervalService.getProfessionalBusyIntervals(query);

        assertTrue(result.isEmpty());
    }

    @Test
    void getIntervalsWithCaching_EmptyCache_FetchesAndReturns() {
        Professional professional = professionalWithFridaySchedule();
        AppointmentTimeWindow window = new AppointmentTimeWindow(FRIDAY, FRIDAY.plusDays(1));
        SalonProfile salonProfile = TestSalonProfileFactory.standard();

        mockAppointments(List.of());
        mockBlocks(List.of());

        AvailabilityQuery query = AvailabilityQuery.builder()
                .professional(professional)
                .window(window)
                .salonProfile(salonProfile)
                .build();

        Map<LocalDate, List<BusyInterval>> result = busyIntervalService.getIntervalsWithCaching(query);

        assertTrue(result.containsKey(FRIDAY));
        assertTrue(result.get(FRIDAY).isEmpty());
    }
}
