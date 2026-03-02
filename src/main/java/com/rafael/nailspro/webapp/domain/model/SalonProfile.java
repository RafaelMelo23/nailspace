package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.nailspro.webapp.domain.enums.salon.OperationalStatus;
import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionConnectionState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
                @UniqueConstraint(name = "uk_salon_tenant_id", columnNames = {"tenant_Id"}),
                @UniqueConstraint(name = "uk_salon_owner_id", columnNames = {"owner_id"})
        }
)
public class SalonProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trade_name", nullable = false, length = 60)
    private String tradeName;

    @Column(name = "slogan", length = 120)
    private String slogan;

    @Column(name = "primary_color", nullable = false, length = 15)
    private String primaryColor;

    @Column(name = "logo_path", nullable = false)
    private String logoPath;

    @Column(name = "comercial_phone", nullable = false, length = 11)
    private String comercialPhone;

    @Column(name = "full_address", nullable = false, length = 80)
    private String fullAddress;

    @Column(name = "social_media_link", length = 50)
    private String socialMediaLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private OperationalStatus operationalStatus = OperationalStatus.OPEN;

    @Column(name = "warning_message", length = 200)
    private String warningMessage;

    @Column(name = "appointment_buffer_minutes", nullable = false)
    private Integer appointmentBufferMinutes;

    @Column(name = "domain_slug", nullable = false, length = 40)
    private String domainSlug;

    @OneToOne(fetch = FetchType.LAZY, optional = false, orphanRemoval = true)
    @JoinColumn(name = "owner_id", nullable = false)
    private Professional owner;

    //todo: add
    @Column(name = "salon_zone_id", nullable = false)
    private ZoneId zoneId;

    @Column(name = "is_loyal_clientele_prioritized", nullable = false)
    private Boolean isLoyalClientelePrioritized = false;

    @Column(name = "loyal_client_booking_window_days")
    private Integer loyalClientBookingWindowDays;

    @Column(name = "standard_booking_window")
    private Integer standardBookingWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_connection_state", nullable = false)
    private EvolutionConnectionState evolutionConnectionState;

    @Column(name = "whatsapp_last_reset_at")
    private LocalDateTime whatsappLastResetAt;

    @Column(name = "last_pairing_code")
    private String lastPairingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status")
    private TenantStatus tenantStatus;

    @Column(name = "auto_confirmation_appointment", nullable = false)
    private boolean autoConfirmationAppointment;

    @Override
    public void prePersist() {
        super.prePersist();

        if (this.tradeName == null || this.tradeName.isBlank()) {
            this.tradeName = "Novo Estabelecimento";
        } else {
            this.tradeName = this.tradeName.trim();
        }

        if (this.primaryColor == null || this.primaryColor.isBlank()) {
            this.primaryColor = "#FB7185";
        }

        if (this.logoPath == null || this.logoPath.isBlank()) {
            this.logoPath = "default-logo.png";
        }

        if (this.comercialPhone == null || this.comercialPhone.isBlank()) {
            this.comercialPhone = "00000000000";
        } else {
            this.comercialPhone = this.comercialPhone.replaceAll("\\D", "");
        }

        if (this.fullAddress == null || this.fullAddress.isBlank()) {
            this.fullAddress = "Endereço a preencher";
        }

        if (this.appointmentBufferMinutes == null) {
            this.appointmentBufferMinutes = 0;
        }

        if (this.operationalStatus == null) {
            this.operationalStatus = OperationalStatus.OPEN;
        }

        if (this.evolutionConnectionState == null) {
            this.evolutionConnectionState = EvolutionConnectionState.CLOSE;
        }

        if (this.tenantStatus == null) {
            this.tenantStatus = TenantStatus.ACTIVE;
        }
    }
}