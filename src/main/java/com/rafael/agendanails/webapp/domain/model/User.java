package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
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

    public void assignId(Long id) {
        this.id = id;
    }

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

    protected User(String fullName, String email, String password, UserStatus status, UserRole userRole) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.status = status;
        this.userRole = userRole;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void updateFullName(String fullName) {
        this.fullName = fullName;
    }

    public void assignTenant(String tenantId) {
        this.setTenantId(tenantId);
    }

    public void updateStatus(UserStatus status) {
        this.status = status;
    }

    protected void setFullName(String fullName) {
        this.fullName = fullName;
    }

    protected void setEmail(String email) {
        this.email = email;
    }

    protected void setPassword(String password) {
        this.password = password;
    }

    protected void setStatus(UserStatus status) {
        this.status = status;
    }

    protected void setUserRole(UserRole userRole) {
        this.userRole = userRole;
    }

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