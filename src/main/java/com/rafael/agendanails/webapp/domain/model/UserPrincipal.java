package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import jakarta.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Builder
@Getter
public class UserPrincipal implements UserDetails {

    private Long userId;
    private String email;
    private List<UserRole> userRole;
    private String tenantId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        userRole.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));

        if (this.userRole.contains(UserRole.SUPER_ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        }

        if (this.userRole.contains(UserRole.ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        }

        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
