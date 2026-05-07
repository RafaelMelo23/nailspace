package com.rafael.agendanails.webapp.domain;

import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.date.SimpleBusyInterval;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus.CONFIRMED;
import static com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus.FINISHED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityDomainService {

    public static final int TIME_SLOT_GAPS = 30;
    private final ProfessionalRepository professionalRepository;
    private final BusyIntervalService busyIntervalService;

    public List<AppointmentTimesDTO> findAvailableTimes(AvailabilityQuery query) {
        log.debug("Finding available times for professional: {}, window: {} to {}, duration: {}",
                query.professional().getId(), query.window().start(), query.window().end(), query.serviceDurationInSeconds());

        if (query.serviceDurationInSeconds() <= 0) {
            return List.of();
        }

        Map<LocalDate, List<BusyInterval>> intervalsByDate = busyIntervalService.getIntervalsWithCaching(query);
        Map<DayOfWeek, WorkSchedule> weeklySchedules = mapWeeklySchedulesByDay(query.professional());

        return query.window().start().datesUntil(query.window().end())
                .map(date -> {
                    DailyWorkContext context = new DailyWorkContext(date, weeklySchedules.get(date.getDayOfWeek()));
                    return buildDailyAvailability(context, intervalsByDate.getOrDefault(date, List.of()), query.serviceDurationInSeconds());
                })
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<AppointmentTimesDTO> buildDailyAvailability(DailyWorkContext context,
                                                                 List<BusyInterval> existingBusyIntervals,
                                                                 int serviceDurationInSeconds) {
        if (context.schedule() == null) {
            return Optional.empty();
        }

        List<BusyInterval> dailyBusy = buildCompleteDailyBusyIntervals(context, existingBusyIntervals);
        List<LocalTime> dailyAvailableTimes = calculateAvailableSlots(context.schedule(), dailyBusy, serviceDurationInSeconds);

        return Optional.of(new AppointmentTimesDTO(context.date(), dailyAvailableTimes));
    }

    private List<BusyInterval> buildCompleteDailyBusyIntervals(DailyWorkContext context,
                                                               List<BusyInterval> existingBusyIntervals) {

        List<BusyInterval> dailyBusy = new ArrayList<>(existingBusyIntervals);

        addLunchBreakToBusyIntervals(context, dailyBusy);

        dailyBusy.sort(Comparator.comparing(BusyInterval::getStart));
        return dailyBusy;
    }

    private void addLunchBreakToBusyIntervals(DailyWorkContext context,
                                              List<BusyInterval> dailyBusy) {

        WorkSchedule workSchedule = context.schedule();
        if (workSchedule.getLunchBreakStartTime() != null && workSchedule.getLunchBreakEndTime() != null) {
            LocalTime lunchStart = workSchedule.getLunchBreakStartTime();
            LocalTime lunchEnd = workSchedule.getLunchBreakEndTime();

            if (lunchStart.isBefore(workSchedule.getWorkEnd()) && lunchEnd.isAfter(workSchedule.getWorkStart())) {
                dailyBusy.add(SimpleBusyInterval.builder()
                        .start(lunchStart.isBefore(workSchedule.getWorkStart()) ? workSchedule.getWorkStart() : lunchStart)
                        .end(lunchEnd.isAfter(workSchedule.getWorkEnd()) ? workSchedule.getWorkEnd() : lunchEnd)
                        .date(context.date())
                        .build());
            }
        }
    }

    private List<LocalTime> calculateAvailableSlots(WorkSchedule workSchedule,
                                                    List<BusyInterval> dailyBusy,
                                                    int serviceDurationInSeconds) {

        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime cursor = workSchedule.getWorkStart();

        for (BusyInterval interval : dailyBusy) {
            if (cursor.isBefore(interval.getStart())) {
                availableTimes.addAll(findGapSlots(cursor, interval.getStart(), serviceDurationInSeconds));
            }

            if (interval.getEnd().isAfter(cursor)) {
                cursor = interval.getEnd();
            }
        }

        if (cursor.isBefore(workSchedule.getWorkEnd())) {
            availableTimes.addAll(findGapSlots(cursor, workSchedule.getWorkEnd(), serviceDurationInSeconds));
        }

        return availableTimes;
    }

    private List<LocalTime> findGapSlots(LocalTime gapStart,
                                         LocalTime gapEnd,
                                         int duration) {

        if (duration <= 0 || ChronoUnit.SECONDS.between(gapStart, gapEnd) < duration) {
            return List.of();
        }

        List<LocalTime> slots = new ArrayList<>();
        LocalTime candidate = gapStart;
        while (!candidate.plusSeconds(duration).isAfter(gapEnd)) {
            slots.add(candidate);
            candidate = candidate.plusMinutes(TIME_SLOT_GAPS);
        }
        return slots;
    }

    private static Map<DayOfWeek, WorkSchedule> mapWeeklySchedulesByDay(Professional professional) {
        return professional.getWorkSchedules().stream()
                .collect(Collectors.toMap(WorkSchedule::getDayOfWeek, ws -> ws));
    }

    public void checkIfProfessionalHasTimeConflicts(UUID professionalId, TimeInterval interval) {
        if (professionalRepository.hasTimeConflicts(professionalId, interval.realTimeStart(), interval.endTimeWithBuffer(), List.of(CONFIRMED, FINISHED))) {
            throw new BusinessException("O profissional já possui um compromisso ou bloqueio neste horário.");
        }
    }
}