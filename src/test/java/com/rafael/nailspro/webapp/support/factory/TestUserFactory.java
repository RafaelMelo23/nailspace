package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Client;

public final class TestUserFactory {

    private TestUserFactory() {}

    public static Client client() {
        return Client.builder()
                .id(1L)
                .tenantId("tenantA")
                .fullName("Test User")
                .email("user@test.local")
                .password("password")
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.CLIENT)
                .phoneNumber("5500000000000")
                .missedAppointments(0)
                .canceledAppointments(0)
                .build();
    }
}