package com.rafael.nailspro.webapp.domain;

import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.model.WorkSchedule;
import com.rafael.nailspro.webapp.domain.repository.AppointmentRepository;
import com.rafael.nailspro.webapp.domain.repository.ProfessionalRepository;
import com.rafael.nailspro.webapp.domain.repository.ScheduleBlockRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AppointmentTimesDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.date.SimpleBusyInterval;
import com.rafael.nailspro.webapp.domain.model.TimeInterval;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.CONFIRMED;
import static com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus.FINISHED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityDomainService {

    public static final int TIME_SLOT_GAPS = 30;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final ProfessionalRepository professionalRepository;

    public List<AppointmentTimesDTO> findAvailableTimes(Professional professional,
                                                        AppointmentTimeWindow window,
                                                        SalonProfile salonProfile,
                                                        int serviceDurationInSeconds) {
        log.debug("Finding available times for professional: {}, window: {} to {}, duration: {}", 
                professional.getId(), window.start(), window.end(), serviceDurationInSeconds);

        if (serviceDurationInSeconds <= 0) {
            return List.of();
        }

        Map<LocalDate, List<BusyInterval>> intervalsByDate = groupBusyIntervalsByDate(professional, window, salonProfile);
        log.debug("Busy intervals count by date: {}", intervalsByDate.size());

        Map<DayOfWeek, WorkSchedule> weeklySchedules = mapWeeklySchedulesByDay(professional);
        log.debug("Weekly schedules days: {}", weeklySchedules.keySet());

        return window.start().datesUntil(window.end())
                .map(date -> {
                    log.debug("Checking date: {}, hasSchedule: {}", date, weeklySchedules.containsKey(date.getDayOfWeek()));
                    return buildDailyAvailability(
                        date,
                        weeklySchedules.get(date.getDayOfWeek()),
                        intervalsByDate.getOrDefault(date, List.of()),
                        serviceDurationInSeconds);
                }
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<AppointmentTimesDTO> buildDailyAvailability(LocalDate date,
                                                                 WorkSchedule workSchedule,
                                                                 List<BusyInterval> existingBusyIntervals,
                                                                 int serviceDurationInSeconds) {
        if (workSchedule == null) {
            return Optional.empty();
        }

        List<BusyInterval> dailyBusy = buildCompleteDailyBusyIntervals(date, existingBusyIntervals, workSchedule);
        List<LocalTime> dailyAvailableTimes = calculateAvailableSlots(workSchedule, dailyBusy, serviceDurationInSeconds);

        return Optional.of(new AppointmentTimesDTO(date, dailyAvailableTimes));
    }

    private List<BusyInterval> buildCompleteDailyBusyIntervals(LocalDate date,
                                                               List<BusyInterval> existingBusyIntervals,
                                                               WorkSchedule workSchedule) {
        List<BusyInterval> dailyBusy = new ArrayList<>(existingBusyIntervals);

        if (workSchedule.getLunchBreakStartTime() != null && workSchedule.getLunchBreakEndTime() != null) {
            LocalTime lunchStart = workSchedule.getLunchBreakStartTime();
            LocalTime lunchEnd = workSchedule.getLunchBreakEndTime();

            if (lunchStart.isBefore(workSchedule.getWorkEnd()) && lunchEnd.isAfter(workSchedule.getWorkStart())) {
                dailyBusy.add(SimpleBusyInterval.builder()
                        .start(lunchStart.isBefore(workSchedule.getWorkStart()) ? workSchedule.getWorkStart() : lunchStart)
                        .end(lunchEnd.isAfter(workSchedule.getWorkEnd()) ? workSchedule.getWorkEnd() : lunchEnd)
                        .date(date)
                        .build());
            }
        }

        dailyBusy.sort(Comparator.comparing(BusyInterval::getStart));
        return dailyBusy;
    }

    private List<LocalTime> calculateAvailableSlots(WorkSchedule workSchedule,
                                                    List<BusyInterval> dailyBusy,
                                                    int serviceDurationInSeconds) {
        List<LocalTime> availableTimes = new ArrayList<>();
        LocalTime cursor = workSchedule.getWorkStart();

        for (BusyInterval interval : dailyBusy) {
            if (cursor.isBefore(interval.getStart())) {
                processTimeSlotGaps(cursor, interval.getStart(), serviceDurationInSeconds, availableTimes);
            }

            if (interval.getEnd().isAfter(cursor)) {
                cursor = interval.getEnd();
            }
        }

        if (cursor.isBefore(workSchedule.getWorkEnd())) {
            processTimeSlotGaps(cursor, workSchedule.getWorkEnd(), serviceDurationInSeconds, availableTimes);
        }

        return availableTimes;
    }

    private Map<LocalDate, List<BusyInterval>> groupBusyIntervalsByDate(Professional professional,
                                                                        AppointmentTimeWindow window,
                                                                        SalonProfile salonProfile) {
        return getProfessionalBusyIntervals(professional, window.start(), window.end(), salonProfile)
                .stream()
                .collect(Collectors.groupingBy(BusyInterval::getDate));
    }

    public List<BusyInterval> getProfessionalBusyIntervals(Professional professional,
                                                           LocalDate startRange,
                                                           LocalDate endRange,
                                                           SalonProfile salonProfile) {
        ZoneId zoneId = salonProfile.getZoneId();
        Instant instantStart = startRange.atStartOfDay(zoneId).toInstant();
        Instant instantEnd = endRange.atStartOfDay(zoneId).toInstant();

        Stream<BusyInterval> appointments = appointmentRepository
                .findBusyAppointmentsInRange(professional.getId(), instantStart, instantEnd, List.of(CONFIRMED, FINISHED))
                .stream()
                .flatMap(app -> mapToDailyIntervals(app.getStartDate(), app.getEndDate(), zoneId, salonProfile.getAppointmentBufferMinutes()));

        Stream<BusyInterval> blocks = scheduleBlockRepository
                .findBusyBlocksInRange(professional.getId(), instantStart, instantEnd)
                .stream()
                .flatMap(block -> mapToDailyIntervals(block.getDateStartTime(), block.getDateEndTime(), zoneId, 0));

        return Stream.concat(appointments, blocks)
                .sorted(Comparator.comparing(BusyInterval::getStart))
                .toList();
    }

    private Stream<BusyInterval> mapToDailyIntervals(Instant start, Instant end, ZoneId zoneId, int bufferMinutes) {
        ZonedDateTime zStart = start.atZone(zoneId);
        ZonedDateTime zEnd = end.atZone(zoneId).plusMinutes(bufferMinutes);

        List<BusyInterval> intervals = new ArrayList<>();
        LocalDate current = zStart.toLocalDate();
        LocalDate last = zEnd.toLocalDate();

        while (!current.isAfter(last)) {
            LocalTime dailyStart = (current.equals(zStart.toLocalDate())) ? zStart.toLocalTime() : LocalTime.MIN;
            LocalTime dailyEnd;

            if (current.isBefore(last)) {
                dailyEnd = LocalTime.MAX;
            } else {
                if (zEnd.toLocalTime().equals(LocalTime.MIN) && !current.equals(zStart.toLocalDate())) {
                    break;
                }
                dailyEnd = zEnd.toLocalTime();
            }

            intervals.add(SimpleBusyInterval.builder()
                    .date(current)
                    .start(dailyStart)
                    .end(dailyEnd)
                    .build());

            current = current.plusDays(1);
        }

        return intervals.stream();
    }

    private void processTimeSlotGaps(LocalTime gapStart,
                                     LocalTime gapEnd,
                                     int duration,
                                     List<LocalTime> suggestions) {
        if (duration <= 0 || ChronoUnit.SECONDS.between(gapStart, gapEnd) < duration) {
            return;
        }

        LocalTime candidate = gapStart;
        while (!candidate.plusSeconds(duration).isAfter(gapEnd)) {
            if (!suggestions.contains(candidate)) {
                suggestions.add(candidate);
            }
            if (candidate.plusMinutes(TIME_SLOT_GAPS).isBefore(candidate)) {
                break;
            }
            candidate = candidate.plusMinutes(TIME_SLOT_GAPS);
        }
    }

    private static Map<DayOfWeek, WorkSchedule> mapWeeklySchedulesByDay(Professional professional) {
        return professional.getWorkSchedules().stream()
                .collect(Collectors.toMap(WorkSchedule::getDayOfWeek, ws -> ws));
    }

    public void checkIfProfessionalHasTimeConflicts(UUID professionalId, TimeInterval interval) {
        LocalDateTime start = interval.toLocalDateTime(interval.realTimeStart());
        LocalDateTime end = interval.toLocalDateTime(interval.endTimeWithBuffer());

        if (professionalRepository.hasTimeConflicts(professionalId, start, end, List.of(CONFIRMED, FINISHED))) {
            throw new BusinessException("O profissional já possui um compromisso ou bloqueio neste horário.");
        }
    }
}