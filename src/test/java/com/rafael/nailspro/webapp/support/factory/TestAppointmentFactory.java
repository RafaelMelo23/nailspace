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

public class TestAppointmentFactory {

    public static Appointment standard(Client client, Professional professional, SalonService mainService) {
        return buildWithStatusAndDates(client, professional, mainService, AppointmentStatus.PENDING, 1);
    }

    public static Appointment past(Client client, Professional professional, SalonService mainService, AppointmentStatus status) {
        return buildWithStatusAndDates(client, professional, mainService, status, -2);
    }

    public static Appointment future(Client client, Professional professional, SalonService mainService, AppointmentStatus status) {
        return buildWithStatusAndDates(client, professional, mainService, status, 1);
    }

    public static Appointment withAddOns(Client client, Professional professional, SalonService mainService, List<AppointmentAddOn> addOns) {
        Appointment appointment = standard(client, professional, mainService);
        appointment.setAddOns(addOns);

        BigDecimal total = BigDecimal.valueOf(mainService.getValue());
        for (AppointmentAddOn addOn : addOns) {
            BigDecimal addOnTotal = BigDecimal.valueOf(addOn.getService().getValue())
                    .multiply(BigDecimal.valueOf(addOn.getQuantity()));
            total = total.add(addOnTotal);
        }
        appointment.setTotalValue(total);

        return appointment;
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
                .id(1L)
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
                .appointmentNotifications(new ArrayList<>())
                .tenantId("tenant-test")
                .build();
    }
}