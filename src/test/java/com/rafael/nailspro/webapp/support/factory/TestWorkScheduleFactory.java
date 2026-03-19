package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.WorkSchedule;
import com.rafael.nailspro.webapp.domain.model.Professional;

import java.time.DayOfWeek;
import java.time.LocalTime;
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

    public static WorkSchedule custom(DayOfWeek dayOfWeek,
                                      LocalTime start,
                                      LocalTime end,
                                      LocalTime lunchStart,
                                      LocalTime lunchEnd,
                                      Professional professional) {
        WorkSchedule builtWs = baseBuilder(dayOfWeek, professional)
                .workStart(start)
                .workEnd(end)
                .lunchBreakStartTime(lunchStart)
                .lunchBreakEndTime(lunchEnd)
                .build();

        return finalizeSchedule(builtWs);
    }

    private static WorkSchedule.WorkScheduleBuilder<?, ?> baseBuilder(
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
