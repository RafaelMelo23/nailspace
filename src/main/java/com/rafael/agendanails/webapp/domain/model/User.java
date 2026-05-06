package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
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
public abstract class User extends BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

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

    public List<UserRole> getEffectiveRoles() {
        return switch (this.userRole) {
            case SUPER_ADMIN -> List.of(UserRole.SUPER_ADMIN, UserRole.ADMIN, UserRole.PROFESSIONAL);
            case ADMIN -> List.of(UserRole.ADMIN, UserRole.PROFESSIONAL);
            case PROFESSIONAL -> List.of(UserRole.PROFESSIONAL);
            case CLIENT -> List.of(UserRole.CLIENT);
        };
    }
}