package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;

import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestSalonProfileFactory {

    public static SalonProfile standard() {
        return baseBuilder().build();
    }

    public static SalonProfile standard(Professional owner) {
        return baseBuilder()
                .owner(owner)
                .build();
    }

    public static SalonProfile withCustomBuffer(int bufferMinutes) {
        return baseBuilder()
                .appointmentBufferMinutes(bufferMinutes)
                .build();
    }

    public static SalonProfile withCustomBufferAndZone(int bufferMinutes, ZoneId zoneId) {
        return baseBuilder()
                .appointmentBufferMinutes(bufferMinutes)
                .zoneId(zoneId)
                .build();
    }

    public static SalonProfile standardForIT(Professional owner) {
        return integrationBuilder()
                .owner(owner)
                .build();
    }

    public static SalonProfile standardForIT(Professional owner, String tenantId) {
        return integrationBuilder()
                .owner(owner)
                .tenantId(tenantId)
                .build();
    }

    public static SalonProfile standardForIT(Professional owner, String tenantId, ZoneId zoneId) {
        return integrationBuilder()
                .owner(owner)
                .tenantId(tenantId)
                .zoneId(zoneId)
                .build();
    }

    public static SalonProfile standardForIT(Professional owner, String tenantId, int bufferMinutes) {
        return integrationBuilder()
                .owner(owner)
                .tenantId(tenantId)
                .appointmentBufferMinutes(bufferMinutes)
                .build();
    }

    public static SalonProfile standardForIT(
            Professional owner,
            String tenantId,
            boolean prioritizeLoyalty,
            int standardWindow,
            int loyalWindow
    ) {
        return integrationBuilder()
                .owner(owner)
                .tenantId(tenantId)
                .isLoyalClientelePrioritized(prioritizeLoyalty)
                .standardBookingWindow(standardWindow)
                .loyalClientBookingWindowDays(loyalWindow)
                .build();
    }

    private static SalonProfile.SalonProfileBuilder<?, ?> baseBuilder() {
        String unique = uniqueSuffix();

        return SalonProfile.builder()
                .id(nextId())
                .tradeName("Test Salon " + unique)
                .tenantId("tenant-" + unique)
                .primaryColor("#FB7185")
                .logoPath("default-logo.png")
                .comercialPhone(generatePhone())
                .fullAddress("Test Address " + unique)
                .operationalStatus(OperationalStatus.OPEN)
                .appointmentBufferMinutes(15)
                .zoneId(ZoneId.of("America/Sao_Paulo"))
                .isLoyalClientelePrioritized(false)
                .standardBookingWindow(7)
                .evolutionConnectionState(EvolutionConnectionState.CLOSE)
                .tenantStatus(TenantStatus.ACTIVE)
                .autoConfirmationAppointment(false);
    }

    private static SalonProfile.SalonProfileBuilder<?, ?> integrationBuilder() {
        String unique = UUID.randomUUID().toString();

        return SalonProfile.builder()
                .id(null)
                .tradeName("IT Salon " + unique)
                .tenantId("tenant-test")
                .primaryColor("#FB7185")
                .logoPath("default-logo.png")
                .comercialPhone(generatePhone())
                .fullAddress("IT Address " + unique)
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

    private static String uniqueSuffix() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10000, 99999));
    }

    private static String generatePhone() {
        return "11" + ThreadLocalRandom.current().nextLong(900000000L, 999999999L);
    }
}