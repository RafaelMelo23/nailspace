package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestAppointmentFactory {

    public static Appointment withNullMainService(
            Client client,
            Professional professional,
            AppointmentStatus status
    ) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(null)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(BigDecimal.ZERO)
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }

    private static Appointment.AppointmentBuilder<?, ?> baseBuilder(
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .appointmentStatus(AppointmentStatus.PENDING)
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test");
    }

    private static Appointment.AppointmentBuilder<?, ?> baseBuilderForIt(
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        return Appointment.builder()
                .id(null)
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .appointmentStatus(AppointmentStatus.PENDING)
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test");
    }

    public static Appointment standardForIt(
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);

        return baseBuilderForIt(client, professional, mainService)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    public static Appointment standard(
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);

        return baseBuilder(client, professional, mainService)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    public static List<Appointment> finishedAppointmentsForIt(
            Client client,
            Professional professional,
            SalonService service,
            int count
    ) {
        List<Appointment> appointments = new ArrayList<>();
        Instant base = Instant.now().minus(count + 1, ChronoUnit.DAYS);

        for (int i = 0; i < count; i++) {
            Instant start = base.plus(i, ChronoUnit.DAYS);
            appointments.add(baseBuilderForIt(client, professional, service)
                    .appointmentStatus(AppointmentStatus.FINISHED)
                    .startDate(start)
                    .endDate(start.plus(1, ChronoUnit.HOURS))
                    .totalValue(BigDecimal.valueOf(service.getValue()))
                    .build());
        }
        return appointments;
    }

    public static Appointment atSpecificTimeForIt(
            Instant start,
            Instant end,
            Client client,
            Professional professional,
            SalonService mainService,
            AppointmentStatus status
    ) {
        return baseBuilderForIt(client, professional, mainService)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(end)
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    public static Appointment atLocalTimeForIt(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            ZoneId zone,
            Client client,
            Professional professional,
            SalonService mainService,
            AppointmentStatus status
    ) {
        Instant startInstant = date.atTime(start).atZone(zone).toInstant();
        Instant endInstant = date.atTime(end).atZone(zone).toInstant();

        return baseBuilderForIt(client, professional, mainService)
                .appointmentStatus(status)
                .startDate(startInstant)
                .endDate(endInstant)
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    public static Appointment atSpecificTimeForIt(Instant start,
                                                  Instant end,
                                                  Professional professional,
                                                  AppointmentStatus status ) {
        return Appointment.builder()
                .id(null)
                .professional(professional)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(end)
                .totalValue(new BigDecimal(150))
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }

    public static List<Appointment> multipleForDay(
            LocalDate date,
            ZoneId zone,
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        return List.of(
                atLocalTimeForIt(date, LocalTime.of(9, 0), LocalTime.of(10, 0), zone, client, professional, mainService, AppointmentStatus.CONFIRMED),
                atLocalTimeForIt(date, LocalTime.of(11, 0), LocalTime.of(12, 0), zone, client, professional, mainService, AppointmentStatus.CONFIRMED),
                atLocalTimeForIt(date, LocalTime.of(15, 0), LocalTime.of(16, 0), zone, client, professional, mainService, AppointmentStatus.CONFIRMED)
        );
    }

    public static List<Appointment> multipleForDay(
            LocalDate date,
            ZoneId zone,
            Client client,
            Professional professional,
            SalonService mainService,
            List<LocalTime[]> intervals
    ) {
        return intervals.stream()
                .map(interval -> atLocalTimeForIt(
                        date,
                        interval[0],
                        interval[1],
                        zone,
                        client,
                        professional,
                        mainService,
                        AppointmentStatus.CONFIRMED
                ))
                .toList();
    }

    public static Appointment overlappingForIt(
            Instant baseStart,
            Client client,
            Professional professional,
            SalonService mainService
    ) {
        return atSpecificTimeForIt(
                baseStart,
                baseStart.plus(1, ChronoUnit.HOURS),
                client,
                professional,
                mainService,
                AppointmentStatus.CONFIRMED
        );
    }

    public static Appointment atSpecificTime(
            Instant start,
            Instant end,
            Professional professional,
            AppointmentStatus status
    ) {
        return Appointment.builder()
                .id(nextId())
                .professional(professional)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(end)
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }

    public static Appointment pastForIt(
            Client client,
            Professional professional,
            SalonService mainService,
            AppointmentStatus status
    ) {
        Instant start = Instant.now().minus(2, ChronoUnit.DAYS);

        return baseBuilderForIt(client, professional, mainService)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    public static Appointment futureForIt(
            Client client,
            Professional professional,
            SalonService mainService,
            AppointmentStatus status
    ) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);

        return baseBuilderForIt(client, professional, mainService)
                .appointmentStatus(status)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .build();
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }

    public static Appointment standardEnglish(
            Client client,
            Professional professional,
            SalonService mainService,
            List<AppointmentAddOn> addOns
    ) {
        List<AppointmentAddOn> safeAddOns =
                addOns == null ? new ArrayList<>() : new ArrayList<>(addOns);

        BigDecimal total = BigDecimal.valueOf(mainService.getValue());

        for (AppointmentAddOn addOn : safeAddOns) {
            BigDecimal addOnTotal = BigDecimal.valueOf(addOn.getService().getValue())
                    .multiply(BigDecimal.valueOf(addOn.getQuantity()));
            total = total.add(addOnTotal);
        }

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);

        return baseBuilder(client, professional, mainService)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(total)
                .addOns(safeAddOns)
                .salonTradeName("Beauty Salon")
                .build();
    }

    public static Appointment withAddOns(
            Client client,
            Professional professional,
            SalonService mainService,
            List<AppointmentAddOn> addOns
    ) {
        List<AppointmentAddOn> safeAddOns =
                addOns == null ? new ArrayList<>() : new ArrayList<>(addOns);

        BigDecimal total = BigDecimal.valueOf(mainService.getValue());

        for (AppointmentAddOn addOn : safeAddOns) {
            BigDecimal addOnTotal = BigDecimal.valueOf(addOn.getService().getValue())
                    .multiply(BigDecimal.valueOf(addOn.getQuantity()));
            total = total.add(addOnTotal);
        }

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);

        return baseBuilder(client, professional, mainService)
                .startDate(start)
                .endDate(start.plus(1, ChronoUnit.HOURS))
                .totalValue(total)
                .addOns(safeAddOns)
                .build();
    }
}