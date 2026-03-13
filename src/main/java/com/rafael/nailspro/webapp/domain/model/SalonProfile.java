package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "salon_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_salon_domain_slug", columnNames = {"domain_slug"}),
                @UniqueConstraint(name = "uk_salon_tenant_id", columnNames = {"tenant_id"}),
                @UniqueConstraint(name = "uk_salon_owner_id", columnNames = {"owner_id"})
        }
)
public class SalonProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trade_name", nullable = false, length = 60)
    @Builder.Default
    private String tradeName = "Novo Estabelecimento";

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "slogan", length = 120)
    private String slogan;

    @Column(name = "primary_color", nullable = false, length = 15)
    @Builder.Default
    private String primaryColor = "#FB7185";

    @Column(name = "logo_path", nullable = false)
    @Builder.Default
    private String logoPath = "default-logo.png";

    @Column(name = "comercial_phone", nullable = false, length = 11)
    @Builder.Default
    private String comercialPhone = "00000000000";

    @Column(name = "full_address", nullable = false, length = 80)
    @Builder.Default
    private String fullAddress = "Endereço a preencher";

    @Column(name = "social_media_link", length = 50)
    private String socialMediaLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    @Builder.Default
    private OperationalStatus operationalStatus = OperationalStatus.OPEN;

    @Column(name = "warning_message", length = 200)
    private String warningMessage;

    @Column(name = "appointment_buffer_minutes", nullable = false)
    @Builder.Default
    private Integer appointmentBufferMinutes = 0;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false, orphanRemoval = true)
    @JoinColumn(name = "owner_id", nullable = false)
    private Professional owner;

    @Column(name = "salon_zone_id", nullable = false)
    @Builder.Default
    private ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    @Column(name = "is_loyal_clientele_prioritized", nullable = false)
    @Builder.Default
    private boolean isLoyalClientelePrioritized = false;

    @Column(name = "loyal_client_booking_window_days")
    private Integer loyalClientBookingWindowDays;

    @Column(name = "standard_booking_window")
    private Integer standardBookingWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_connection_state", nullable = false)
    @Builder.Default
    private EvolutionConnectionState evolutionConnectionState = EvolutionConnectionState.CLOSE;

    @Column(name = "whatsapp_last_reset_at")
    private LocalDateTime whatsappLastResetAt;

    @Column(name = "last_pairing_code")
    private String lastPairingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status")
    @Builder.Default
    private TenantStatus tenantStatus = TenantStatus.ACTIVE;

    @Column(name = "auto_confirmation_appointment", nullable = false)
    @Builder.Default
    private boolean autoConfirmationAppointment = false;

    @PrePersist
    @PreUpdate
    public void sanitizeData() {
        if (this.tradeName != null) {
            this.tradeName = this.tradeName.trim();
        }

        if (this.standardBookingWindow == null) {
            this.standardBookingWindow = 7;
        }

        if (this.comercialPhone != null) {
            this.comercialPhone = this.comercialPhone.replaceAll("\\D", "");
        }
    }
}