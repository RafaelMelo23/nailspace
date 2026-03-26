package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.model.AppointmentAddOn;
import com.rafael.nailspro.webapp.domain.model.SalonService;

import java.util.concurrent.ThreadLocalRandom;

public class TestAppointmentAddOnFactory {

    public static AppointmentAddOn standard(SalonService service) {
        return baseBuilder(service)
                .quantity(1)
                .unitPriceSnapshot(service.getValue())
                .tenantId("tenant-test")
                .build();
    }

    public static AppointmentAddOn standardEnglish(SalonService service) {
        return baseBuilder(service)
                .quantity(1)
                .unitPriceSnapshot(service.getValue())
                .tenantId("tenant-123")
                .build();
    }

    public static AppointmentAddOn withQuantity(SalonService service, int quantity) {
        return baseBuilder(service)
                .quantity(quantity)
                .unitPriceSnapshot(service.getValue())
                .tenantId("tenant-test")
                .build();
    }

    public static AppointmentAddOn withSnapshotPrice(SalonService service, int unitPriceSnapshot) {
        return baseBuilder(service)
                .quantity(1)
                .unitPriceSnapshot(unitPriceSnapshot)
                .tenantId("tenant-test")
                .build();
    }

    private static AppointmentAddOn.AppointmentAddOnBuilder<?, ?> baseBuilder(SalonService service) {
        return AppointmentAddOn.builder()
                .id(nextId())
                .service(service)
                .tenantId("tenant-test")
                .quantity(1)
                .unitPriceSnapshot(service.getValue());
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
