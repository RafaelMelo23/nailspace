package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.AppointmentAddOn;
import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestAppointmentFactory {

    public static Appointment standardForIt(Client client, Professional professional, SalonService mainService) {
        Instant startDate = Instant.now().plus(1, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(null)
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .appointmentStatus(AppointmentStatus.PENDING)
                .startDate(startDate)
                .endDate(startDate.plus(1, ChronoUnit.HOURS))
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }

    public static Appointment standard(Client client, Professional professional, SalonService mainService) {
        return buildWithStatusAndDates(client, professional, mainService, AppointmentStatus.PENDING, 1);
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

        Instant baseDate = Instant.now().plus(1, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .totalValue(total)
                .appointmentStatus(AppointmentStatus.PENDING)
                .startDate(baseDate)
                .endDate(baseDate.plus(1, ChronoUnit.HOURS))
                .salonTradeName("Beauty Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(safeAddOns)
                .tenantId("tenant-123")
                .build();
    }

    public static Appointment past(Client client, Professional professional, SalonService mainService, AppointmentStatus status) {
        return buildWithStatusAndDates(client, professional, mainService, status, -2);
    }

    public static Appointment future(Client client, Professional professional, SalonService mainService, AppointmentStatus status) {
        return buildWithStatusAndDates(client, professional, mainService, status, 1);
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

        Instant baseDate = Instant.now().plus(1, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .totalValue(total)
                .appointmentStatus(AppointmentStatus.PENDING)
                .startDate(baseDate)
                .endDate(baseDate.plus(1, ChronoUnit.HOURS))
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(safeAddOns)
                .tenantId("tenant-test")
                .build();
    }

    private static Appointment buildWithStatusAndDates(
            Client client,
            Professional professional,
            SalonService mainService,
            AppointmentStatus status,
            int daysOffset
    ) {

        Instant baseDate = Instant.now().plus(daysOffset, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .totalValue(BigDecimal.valueOf(mainService.getValue()))
                .appointmentStatus(status)
                .startDate(baseDate)
                .endDate(baseDate.plus(1, ChronoUnit.HOURS))
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }

    public static Appointment withNullMainService(
            Client client,
            Professional professional,
            AppointmentStatus status
    ) {
        Instant baseDate = Instant.now().plus(1, ChronoUnit.DAYS);

        return Appointment.builder()
                .id(nextId())
                .client(client)
                .professional(professional)
                .mainSalonService(null)
                .totalValue(BigDecimal.ZERO)
                .appointmentStatus(status)
                .startDate(baseDate)
                .endDate(baseDate.plus(1, ChronoUnit.HOURS))
                .salonTradeName("Test Salon")
                .salonZoneId(ZoneId.of("America/Sao_Paulo"))
                .addOns(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
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

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
