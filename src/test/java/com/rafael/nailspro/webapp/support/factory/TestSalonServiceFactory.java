package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonService;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestSalonServiceFactory {

    private TestSalonServiceFactory() {}

    public static SalonService.SalonServiceBuilder<?, ?> builder() {
        return SalonService.builder()
                .name("Service-" + UUID.randomUUID())
                .description("Desc-" + UUID.randomUUID())
                .durationInSeconds(3600)
                .value(50)
                .active(true)
                .maintenanceIntervalDays(15)
                .isAddOn(false)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test");
    }

    public static SalonService standard() {
        return builder().id(nextId()).build();
    }

    public static SalonService manicure() {
        return builder()
                .id(nextId())
                .name("Manicure")
                .tenantId("tenant-123")
                .build();
    }

    public static SalonService pedicure() {
        return builder()
                .id(nextId())
                .name("Pedicure")
                .tenantId("tenant-123")
                .build();
    }

    public static SalonService standardForIt() {
        return builder().build();
    }

    public static SalonService standardForIt(int durationSeconds, Integer maintenanceIntervalDays) {
        return builder()
                .durationInSeconds(durationSeconds)
                .maintenanceIntervalDays(maintenanceIntervalDays)
                .build();
    }

    public static SalonService standardForIt(String tenantId) {
        return builder().tenantId(tenantId).build();
    }

    public static SalonService standardForIt(String tenantId, int durationSeconds, Integer maintenanceIntervalDays) {
        return builder()
                .tenantId(tenantId)
                .durationInSeconds(durationSeconds)
                .maintenanceIntervalDays(maintenanceIntervalDays)
                .build();
    }

    public static SalonService withCustomValue(Integer value) {
        return builder().id(nextId()).value(value).build();
    }

    public static SalonService standardWithoutMaintenanceInterval() {
        return builder().id(nextId()).maintenanceIntervalDays(null).build();
    }

    public static SalonService addOnWithoutMaintenanceInterval() {
        return builder()
                .id(nextId())
                .isAddOn(true)
                .nailCount(1)
                .durationInSeconds(900)
                .value(15)
                .maintenanceIntervalDays(null)
                .build();
    }

    public static SalonService addOnWithMaintenanceInterval() {
        return addOnWithMaintenanceInterval(10);
    }

    public static SalonService addOnWithMaintenanceInterval(int days) {
        return builder()
                .id(nextId())
                .isAddOn(true)
                .nailCount(1)
                .durationInSeconds(900)
                .value(15)
                .maintenanceIntervalDays(days)
                .build();
    }

    public static SalonService withMaintenanceInterval(int days) {
        return builder().id(nextId()).maintenanceIntervalDays(days).build();
    }

    public static SalonService withProfessionals(Set<Professional> professionals) {
        return builder().id(nextId()).professionals(professionals).build();
    }

    public static SalonService withDuration(int durationSeconds) {
        return builder().id(nextId()).durationInSeconds(durationSeconds).build();
    }

    public static SalonService withInterval(Integer interval) {
        return builder().id(nextId()).maintenanceIntervalDays(interval).build();
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}