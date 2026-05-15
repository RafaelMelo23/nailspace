package com.rafael.agendanails.webapp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refresh_token_per_tenant",
                        columnNames = {"tenantId", "token"})
        })
public class RefreshToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    @Column(name = "is_revoked")
    private boolean isRevoked;

    public RefreshToken(User user, String token, Instant expiryDate) {
        this.user = user;
        this.token = token;
        this.expiryDate = expiryDate;
        this.isRevoked = false;
        if (user != null) {
            this.setTenantId(user.getTenantId());
        }
    }

    public void revoke() {
        this.isRevoked = true;
    }
}