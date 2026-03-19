package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;

import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

public class TestSalonProfileFactory {

    public static SalonProfile standard() {
        return baseBuilder().build();
    }

    public static SalonProfile standard(Professional owner) {
        SalonProfile profile = standard();
        profile.setOwner(owner);
        return profile;
    }

    public static SalonProfile withCustomBuffer(int bufferMinutes) {
        SalonProfile profile = standard();
        profile.setAppointmentBufferMinutes(bufferMinutes);
        return profile;
    }

    public static SalonProfile withCustomBufferAndZone(int bufferMinutes, ZoneId zoneId) {
        SalonProfile profile = standard();
        profile.setAppointmentBufferMinutes(bufferMinutes);
        profile.setZoneId(zoneId);
        return profile;
    }

    private static SalonProfile.SalonProfileBuilder<?, ?> baseBuilder() {
        return SalonProfile.builder()
                .id(nextId())
                .tradeName("Test Salon")
                .tenantId("tenant-test")
                .primaryColor("#FB7185")
                .logoPath("default-logo.png")
                .comercialPhone("11999999999")
                .fullAddress("Test Address 123")
                .operationalStatus(OperationalStatus.OPEN)
                .appointmentBufferMinutes(15)
                .zoneId(ZoneId.of("America/Sao_Paulo"))
                .isLoyalClientelePrioritized(false)
                .standardBookingWindow(7)
                .evolutionConnectionState(EvolutionConnectionState.CLOSE)
                .tenantStatus(TenantStatus.ACTIVE)
                .autoConfirmationAppointment(false);
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
