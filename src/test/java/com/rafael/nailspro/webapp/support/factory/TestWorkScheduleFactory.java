package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.WorkSchedule;
import com.rafael.nailspro.webapp.domain.model.Professional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestWorkScheduleFactory {

    public static WorkSchedule standard(DayOfWeek dayOfWeek, Professional professional) {
        WorkSchedule builtWs = baseBuilder(dayOfWeek, professional)
                .workStart(LocalTime.of(9, 0))
                .workEnd(LocalTime.of(18, 0))
                .lunchBreakStartTime(LocalTime.of(12, 0))
                .lunchBreakEndTime(LocalTime.of(13, 0))
                .build();

        return finalizeSchedule(builtWs);
    }

    public static WorkSchedule withoutLunch(DayOfWeek dayOfWeek, Professional professional) {
        WorkSchedule builtWs = baseBuilder(dayOfWeek, professional)
                .workStart(LocalTime.of(8, 0))
                .workEnd(LocalTime.of(14, 0))
                .lunchBreakStartTime(null)
                .lunchBreakEndTime(null)
                .build();

        return finalizeSchedule(builtWs);
    }

    public static WorkSchedule custom(
            DayOfWeek dayOfWeek,
            LocalTime start,
            LocalTime end,
            LocalTime lunchStart,
            LocalTime lunchEnd,
            Professional professional
    ) {
        WorkSchedule builtWs = baseBuilder(dayOfWeek, professional)
                .workStart(start)
                .workEnd(end)
                .lunchBreakStartTime(lunchStart)
                .lunchBreakEndTime(lunchEnd)
                .build();

        return finalizeSchedule(builtWs);
    }

    public static WorkSchedule customForIt(
            DayOfWeek dayOfWeek,
            LocalTime start,
            LocalTime end,
            LocalTime lunchStart,
            LocalTime lunchEnd,
            Professional professional
    ) {
        WorkSchedule builtWs = baseBuilder(dayOfWeek, professional)
                .workStart(start)
                .workEnd(end)
                .lunchBreakStartTime(lunchStart)
                .lunchBreakEndTime(lunchEnd)
                .build();

        builtWs.setTenantId("tenant-test");

        return builtWs;
    }

    public static List<WorkSchedule> fullWeek(Professional professional) {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> standard(day, professional))
                .toList();
    }

    public static List<WorkSchedule> fullWeekForIt(Professional professional) {
        return fullWeekForIt(
                professional,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0)
        );
    }

    public static List<WorkSchedule> fullWeekForIt(
            Professional professional,
            LocalTime workStart,
            LocalTime workEnd,
            LocalTime lunchStart,
            LocalTime lunchEnd
    ) {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> customForIt(
                        day,
                        workStart,
                        workEnd,
                        lunchStart,
                        lunchEnd,
                        professional
                ))
                .toList();
    }

    public static List<WorkSchedule> weekDaysOnly(Professional professional) {
        return List.of(
                standard(DayOfWeek.MONDAY, professional),
                standard(DayOfWeek.TUESDAY, professional),
                standard(DayOfWeek.WEDNESDAY, professional),
                standard(DayOfWeek.THURSDAY, professional),
                standard(DayOfWeek.FRIDAY, professional)
        );
    }

    public static List<WorkSchedule> customWeek(
            Professional professional,
            LocalTime start,
            LocalTime end,
            LocalTime lunchStart,
            LocalTime lunchEnd
    ) {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> custom(day, start, end, lunchStart, lunchEnd, professional))
                .toList();
    }

    private static WorkSchedule.WorkScheduleBuilder baseBuilder(
            DayOfWeek dayOfWeek,
            Professional professional
    ) {
        return WorkSchedule.builder()
                .dayOfWeek(dayOfWeek)
                .isActive(true)
                .professional(professional);
    }

    private static WorkSchedule finalizeSchedule(WorkSchedule schedule) {
        schedule.setId(nextId());
        schedule.setTenantId("tenant-test");
        return schedule;
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}