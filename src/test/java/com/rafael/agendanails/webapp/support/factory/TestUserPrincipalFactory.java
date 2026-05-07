package com.rafael.agendanails.webapp.support.factory;

import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;

import java.util.List;

public class TestUserPrincipalFactory {

    public static UserPrincipal from(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .userRole(List.of(user.getUserRole()))
                .userStatus(user.getStatus())
                .tenantId(user.getTenantId())
                .build();
    }
}
