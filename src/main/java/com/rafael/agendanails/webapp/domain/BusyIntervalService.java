package com.rafael.agendanails.webapp.domain;

import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.model.AvailabilityQuery;
import com.rafael.agendanails.webapp.domain.model.BusyIntervalRange;
import com.rafael.agendanails.webapp.domain.model.ScheduleBlock;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.domain.repository.ScheduleBlockRepository;
import com.rafael.agendanails.webapp.infrastructure.config.CacheConfig;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.contract.BusyInterval;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.date.SimpleBusyInterval;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus.CONFIRMED;
import static com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus.FINISHED;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusyIntervalService {

    private final AppointmentRepository appointmentRepository;
    private final ScheduleBlockRepository scheduleBlockRepository;
    private final CacheManager cacheManager;

    public Map<LocalDate, List<BusyInterval>> getIntervalsWithCaching(AvailabilityQuery query) {
        Cache cache = cacheManager.getCache(CacheConfig.BUSY_INTERVALS_CACHE);
        Map<LocalDate, List<BusyInterval>> results = new HashMap<>();
        List<LocalDate> missingDates = new ArrayList<>();

        lookupCachedIntervals(query, cache, results, missingDates);

        if (!missingDates.isEmpty()) {
            fetchAndCacheMissingIntervals(query, cache, results, missingDates);
        }

        return results;
    }

    private void lookupCachedIntervals(AvailabilityQuery query,
                                       Cache cache,
                                       Map<LocalDate, List<BusyInterval>> results,
                                       List<LocalDate> missingDates) {

        query.window().start().datesUntil(query.window().end()).forEach(date -> {
            String key = buildCacheKey(query.professional().getExternalId(), date);
            List<BusyInterval> cached = cache != null ? cache.get(key, List.class) : null;

            if (cached != null) {
                results.put(date, cached);
            } else {
                missingDates.add(date);
            }
        });
    }

    private void fetchAndCacheMissingIntervals(AvailabilityQuery query,
                                               Cache cache,
                                               Map<LocalDate, List<BusyInterval>> results,
                                               List<LocalDate> missingDates) {
        AppointmentTimeWindow missRange = new AppointmentTimeWindow(
                missingDates.getFirst(),
                missingDates.getLast().plusDays(1)
        );

        AvailabilityQuery fetchQuery = AvailabilityQuery.builder()
                .professional(query.professional())
                .salonProfile(query.salonProfile())
                .window(missRange)
                .build();

        Map<LocalDate, List<BusyInterval>> fetched = groupBusyIntervalsByDate(fetchQuery);

        for (LocalDate date : missingDates) {
            List<BusyInterval> daily = fetched.getOrDefault(date, List.of());
            results.put(date, daily);
            if (cache != null) {
                cache.put(buildCacheKey(query.professional().getExternalId(), date), daily);
            }
        }
    }

    private String buildCacheKey(UUID professionalId, LocalDate date) {
        return professionalId + ":" + date;
    }

    public void clearAvailabilityCache(UUID professionalId, LocalDate date) {
        Cache cache = cacheManager.getCache(CacheConfig.BUSY_INTERVALS_CACHE);
        if (cache != null) {
            String key = buildCacheKey(professionalId, date);
            cache.evict(key);
            log.debug("Evicted availability cache for key: {}", key);
        }
    }

    public void evictCacheForAppointment(Appointment appointment, ZoneId zoneId) {
        evictRange(appointment.getProfessional().getExternalId(), appointment.getStartDate(), appointment.getEndDate(), zoneId);
    }

    public void evictCacheForBlock(ScheduleBlock block, ZoneId zoneId) {
        evictRange(block.getProfessional().getExternalId(), block.getStartTime(), block.getEndTime(), zoneId);
    }

    private void evictRange(UUID professionalId, Instant start, Instant end, ZoneId zoneId) {
        LocalDate startDate = start.atZone(zoneId).toLocalDate();
        LocalDate endDate = end.atZone(zoneId).toLocalDate();

        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> clearAvailabilityCache(professionalId, date));
    }

    private Map<LocalDate, List<BusyInterval>> groupBusyIntervalsByDate(AvailabilityQuery query) {
        return getProfessionalBusyIntervals(query)
                .stream()
                .collect(Collectors.groupingBy(BusyInterval::getDate));
    }

    public List<BusyInterval> getProfessionalBusyIntervals(AvailabilityQuery query) {
        ZoneId zoneId = query.salonProfile().getZoneId();
        BusyIntervalRange range = new BusyIntervalRange(
                query.window().start().atStartOfDay(zoneId).toInstant(),
                query.window().end().atStartOfDay(zoneId).toInstant()
        );

        Stream<BusyInterval> appointments = fetchAppointmentsAsIntervals(query, range);
        Stream<BusyInterval> blocks = fetchBlocksAsIntervals(query, range);

        return Stream.concat(appointments, blocks)
                .sorted(Comparator.comparing(BusyInterval::getStart))
                .toList();
    }

    private Stream<BusyInterval> fetchAppointmentsAsIntervals(AvailabilityQuery query,
                                                              BusyIntervalRange range) {
        return appointmentRepository
                .findBusyAppointmentsInRange(query.professional().getId(),
                        range.start(),
                        range.end(),
                        List.of(CONFIRMED, FINISHED))
                .stream()
                .flatMap(app -> mapToDailyIntervals(new BusyIntervalRange(app.getStartDate(), app.getEndDate()),
                        query.salonProfile().getZoneId(),
                        query.salonProfile().getAppointmentBufferMinutes()));
    }

    private Stream<BusyInterval> fetchBlocksAsIntervals(AvailabilityQuery query,
                                                        BusyIntervalRange range) {
        return scheduleBlockRepository
                .findBusyBlocksInRange(query.professional().getId(), range.start(), range.end())
                .stream()
                .flatMap(block -> mapToDailyIntervals(new BusyIntervalRange(block.getStartTime(), block.getEndTime()),
                        query.salonProfile().getZoneId(), 0));
    }

    private Stream<BusyInterval> mapToDailyIntervals(BusyIntervalRange range,
                                                     ZoneId zoneId,
                                                     int bufferMinutes) {

        ZonedDateTime zStart = range.start().atZone(zoneId);
        ZonedDateTime zEnd = range.end().atZone(zoneId).plusMinutes(bufferMinutes);

        List<BusyInterval> intervals = new ArrayList<>();
        LocalDate current = zStart.toLocalDate();
        LocalDate last = zEnd.toLocalDate();

        while (!current.isAfter(last)) {
            LocalTime dailyStart = (current.equals(zStart.toLocalDate())) ? zStart.toLocalTime() : LocalTime.MIN;
            LocalTime dailyEnd = calculateDailyEnd(current, last, zEnd, zStart.toLocalDate());

            if (dailyEnd == null) break;

            intervals.add(SimpleBusyInterval.builder()
                    .date(current)
                    .start(dailyStart)
                    .end(dailyEnd)
                    .build());

            current = current.plusDays(1);
        }
        return intervals.stream();
    }

    private LocalTime calculateDailyEnd(LocalDate current,
                                        LocalDate last,
                                        ZonedDateTime zEnd,
                                        LocalDate startDate) {
        if (current.isBefore(last)) {
            return LocalTime.MAX;
        } else {
            if (zEnd.toLocalTime().equals(LocalTime.MIN) && !current.equals(startDate)) {
                return null;
            }
            return zEnd.toLocalTime();
        }
    }
}