package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import com.rafael.nailspro.webapp.domain.model.Professional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class TestScheduleBlockFactory {

    public static ScheduleBlock atSpecificTime(Instant start, Instant end, Professional professional) {
        return ScheduleBlock.builder()
                .id(nextId())
                .dateStartTime(start)
                .dateEndTime(end)
                .isWholeDayBlocked(false)
                .reason("Test Block")
                .professional(professional)
                .tenantId("tenant-test")
                .build();
    }

    public static ScheduleBlock wholeDay(Instant startOfDay, Professional professional) {
        return ScheduleBlock.builder()
                .id(nextId())
                .dateStartTime(startOfDay)
                .dateEndTime(startOfDay.plus(24, ChronoUnit.HOURS))
                .isWholeDayBlocked(true)
                .reason("Day Off")
                .professional(professional)
                .tenantId("tenant-test")
                .build();
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
