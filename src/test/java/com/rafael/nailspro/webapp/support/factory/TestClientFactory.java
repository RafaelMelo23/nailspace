package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Client;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class TestClientFactory {

    private TestClientFactory() {}

    public static Client.ClientBuilder<?, ?> builder() {
        String unique = UUID.randomUUID().toString();
        return Client.builder()
                .fullName("Test User " + unique)
                .email("user+" + unique + "@test.local")
                .password("password")
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.CLIENT)
                .phoneNumber(generateRandomPhoneNumber())
                .missedAppointments(0)
                .canceledAppointments(0)
                .tenantId("tenant-test");
    }

    public static Client standard() {
        return builder().id(nextId()).build();
    }

    public static Client standardEnglish() {
        return builder()
                .id(nextId())
                .fullName("Jane Doe")
                .email("jane.doe@test.com")
                .tenantId("tenant-123")
                .build();
    }

    public static Client standardForIt() {
        return builder().build();
    }

    public static Client standardForIt(String tenantId) {
        return builder().tenantId(tenantId).build();
    }

    private static String generateRandomPhoneNumber() {
        return ThreadLocalRandom.current()
                .ints(13, 0, 10)
                .mapToObj(Integer::toString)
                .collect(Collectors.joining());
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}