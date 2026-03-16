package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@SuperBuilder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_per_tenant",
                        columnNames = {"tenant_Id", "email"}),
                @UniqueConstraint(name = "uk_phone_per_tenant",
                        columnNames = {"tenant_id", "phone_number"}),
                @UniqueConstraint(
                        name = "uk_professional_external_id_per_tenant",
                        columnNames = {"tenant_id", "external_id"})
        })
public abstract class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue
    private Long id;

    private String fullName;
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Override
    public void prePersist() {
        super.prePersist();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.userRole.name()));

        if (this.userRole == UserRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        }

        if (this.userRole == UserRole.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
        }

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}