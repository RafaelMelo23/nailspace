package com.rafael.agendanails.webapp.infrastructure.security.sentry;

import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import io.sentry.protocol.User;
import io.sentry.spring.jakarta.SentryUserProvider;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SentryUserProviderImpl implements SentryUserProvider {

    @Override
    public User provideUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();

            if (principal instanceof UserPrincipal userPrincipal) {
                User user = new User();

                user.setId(String.valueOf(userPrincipal.getId()));
                user.setEmail(userPrincipal.getEmail());

                Map<String, String> data = new HashMap<>();
                userPrincipal.getUserRole().forEach(role -> data.put("role", role.name()));
                data.put("tenantId", userPrincipal.getTenantId());

                user.setData(data);
                return user;
            }
        }

        return null;
    }
}