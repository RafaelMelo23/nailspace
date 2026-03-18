package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonService;

import java.util.LinkedHashSet;
import java.util.Set;

public class TestSalonServiceFactory {

    public static SalonService standard() {
        return SalonService.builder()
                .id(1L)
                .name("Manicure Padrão")
                .description("Cutilagem e esmaltação simples")
                .durationInSeconds(3600) // 1 hour
                .value(50)
                .active(true)
                .maintenanceIntervalDays(15)
                .requiresLoyalty(false)
                .isAddOn(false)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test")
                .build();
    }

    public static SalonService standardWithoutMaintenanceInterval() {
        return SalonService.builder()
                .id(1L)
                .name("Manicure Padrão")
                .description("Cutilagem e esmaltação simples")
                .durationInSeconds(3600) // 1 hour
                .value(50)
                .active(true)
                .maintenanceIntervalDays(null)
                .requiresLoyalty(false)
                .isAddOn(false)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test")
                .build();
    }

    public static SalonService addOnWithoutMaintenanceInterval() {
        return SalonService.builder()
                .id(2L)
                .name("Nail Art")
                .description("Arte em uma unha")
                .nailCount(1)
                .durationInSeconds(900) // 15 mins
                .value(15)
                .active(true)
                .maintenanceIntervalDays(null)
                .requiresLoyalty(false)
                .isAddOn(true)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test")
                .build();
    }

    public static SalonService addOnWithMaintenanceInterval() {
        return SalonService.builder()
                .id(2L)
                .name("Nail Art")
                .description("Arte em uma unha")
                .nailCount(1)
                .durationInSeconds(900) // 15 mins
                .value(15)
                .active(true)
                .maintenanceIntervalDays(10)
                .requiresLoyalty(false)
                .isAddOn(true)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test")
                .build();
    }

    public static SalonService addOnWithMaintenanceInterval(int days) {
        return SalonService.builder()
                .id(2L)
                .name("Nail Art")
                .description("Arte em uma unha")
                .nailCount(1)
                .durationInSeconds(900) // 15 mins
                .value(15)
                .active(true)
                .maintenanceIntervalDays(days)
                .requiresLoyalty(false)
                .isAddOn(true)
                .professionals(new LinkedHashSet<>())
                .tenantId("tenant-test")
                .build();
    }

    public static SalonService withMaintenanceInterval(int days) {
        SalonService service = standard();
        service.setMaintenanceIntervalDays(days);
        return service;
    }

    public static SalonService withProfessionals(Set<Professional> professionals) {
        SalonService service = standard();
        service.setProfessionals(professionals);
        return service;
    }
}