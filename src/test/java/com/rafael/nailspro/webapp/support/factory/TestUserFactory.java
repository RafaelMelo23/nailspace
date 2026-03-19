package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Client;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TestUserFactory {

    private TestUserFactory() {}

    public static Client client() {
        String unique = UUID.randomUUID().toString();
        return Client.builder()
                .id(nextId())
                .tenantId("tenantA")
                .fullName("Test User " + unique)
                .email("user+" + unique + "@test.local")
                .password("password")
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.CLIENT)
                .phoneNumber("5500000000000")
                .missedAppointments(0)
                .canceledAppointments(0)
                .build();
    }

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}
