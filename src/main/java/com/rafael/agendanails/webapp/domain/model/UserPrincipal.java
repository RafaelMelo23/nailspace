package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
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

    private Long id;
    private String password;
    private String email;
    private List<UserRole> userRole;
    private UserStatus userStatus;
    private String tenantId;
    private boolean isFirstLogin;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        
        userRole.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name())));

        applyRoleHierarchy(authorities);

        return authorities;
    }

    private void applyRoleHierarchy(List<SimpleGrantedAuthority> authorities) {
        if (this.userRole.contains(UserRole.SUPER_ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        } else if (this.userRole.contains(UserRole.ADMIN)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        }
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return userStatus != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isProfessional() {
        return userRole.contains(UserRole.PROFESSIONAL);
    }
}
