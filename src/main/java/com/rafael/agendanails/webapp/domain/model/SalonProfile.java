package com.rafael.agendanails.webapp.domain.model;

import com.rafael.agendanails.webapp.domain.enums.appointment.TenantStatus;
import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionConnectionState;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "salon_profile",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_salon_domain_slug", columnNames = {"domain_slug"}),
                @UniqueConstraint(name = "uk_salon_tenant_id", columnNames = {"tenant_id"}),
                @UniqueConstraint(name = "uk_salon_owner_id", columnNames = {"owner_id"})
        }
)
public class SalonProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "trade_name", nullable = false, length = 60)
    private String tradeName = "Novo Estabelecimento";

    @Column(name = "slogan", length = 120)
    private String slogan;

    @Column(name = "primary_color", nullable = false, length = 15)
    private String primaryColor = "#FB7185";

    @Column(name = "logo_path", nullable = false)
    private String logoPath = "default-logo.png";

    @Column(name = "comercial_phone", nullable = false, length = 13)
    private String comercialPhone = "00000000000";

    @Column(name = "full_address", nullable = false, length = 80)
    private String fullAddress = "Endereço a preencher";

    @Column(name = "social_media_link", length = 50)
    private String socialMediaLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private OperationalStatus operationalStatus = OperationalStatus.OPEN;

    @Column(name = "warning_message", length = 200)
    private String warningMessage;

    @Column(name = "appointment_buffer_minutes", nullable = false)
    private Integer appointmentBufferMinutes = 0;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY, optional = false, orphanRemoval = true)
    @JoinColumn(name = "owner_id", nullable = false)
    private Professional owner;

    @Column(name = "salon_zone_id", nullable = false)
    private ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    @Column(name = "is_loyal_clientele_prioritized", nullable = false)
    private boolean isLoyalClientelePrioritized = false;

    @Column(name = "loyal_client_booking_window_days")
    private Integer loyalClientBookingWindowDays;

    @Column(name = "standard_booking_window")
    private Integer standardBookingWindow;

    @Enumerated(EnumType.STRING)
    @Column(name = "evolution_connection_state", nullable = false)
    private EvolutionConnectionState evolutionConnectionState = EvolutionConnectionState.CLOSE;

    @Column(name = "whatsapp_last_reset_at")
    private LocalDateTime whatsappLastResetAt;

    @Column(name = "last_pairing_code")
    private String lastPairingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_status")
    private TenantStatus tenantStatus = TenantStatus.ACTIVE;

    @Column(name = "auto_confirmation_appointment", nullable = false)
    private boolean autoConfirmationAppointment = false;

    public SalonProfile(Professional owner, String tenantId) {
        super(tenantId);
        this.owner = owner;
    }

    @Builder(builderMethodName = "testBuilder")
    public SalonProfile(Long id, String tradeName, String slogan, String primaryColor, String logoPath, String comercialPhone, String fullAddress, String socialMediaLink, OperationalStatus operationalStatus, String warningMessage, Integer appointmentBufferMinutes, Professional owner, ZoneId zoneId, boolean isLoyalClientelePrioritized, Integer loyalClientBookingWindowDays, Integer standardBookingWindow, EvolutionConnectionState evolutionConnectionState, LocalDateTime whatsappLastResetAt, String lastPairingCode, TenantStatus tenantStatus, boolean autoConfirmationAppointment, String tenantId) {
        super(tenantId);
        this.id = id;
        this.tradeName = tradeName;
        this.slogan = slogan;
        this.primaryColor = primaryColor;
        this.logoPath = logoPath;
        this.comercialPhone = comercialPhone;
        this.fullAddress = fullAddress;
        this.socialMediaLink = socialMediaLink;
        this.operationalStatus = operationalStatus;
        this.warningMessage = warningMessage;
        this.appointmentBufferMinutes = appointmentBufferMinutes;
        this.owner = owner;
        this.zoneId = zoneId;
        this.isLoyalClientelePrioritized = isLoyalClientelePrioritized;
        this.loyalClientBookingWindowDays = loyalClientBookingWindowDays;
        this.standardBookingWindow = standardBookingWindow;
        this.evolutionConnectionState = evolutionConnectionState;
        this.whatsappLastResetAt = whatsappLastResetAt;
        this.lastPairingCode = lastPairingCode;
        this.tenantStatus = tenantStatus;
        this.autoConfirmationAppointment = autoConfirmationAppointment;
    }

    public void assignOwner(Professional owner) {
        this.owner = owner;
    }

    public void updateBasicInfo(String tradeName, String slogan, String comercialPhone, String fullAddress, String socialMediaLink) {
        if (tradeName != null) this.tradeName = tradeName.trim();
        if (slogan != null) this.slogan = slogan;
        if (comercialPhone != null) this.comercialPhone = comercialPhone.replaceAll("\\D", "");
        if (fullAddress != null) this.fullAddress = fullAddress;
        if (socialMediaLink != null) this.socialMediaLink = socialMediaLink;
    }

    public void updateAppearance(String primaryColor, String logoPath) {
        if (primaryColor != null) this.primaryColor = primaryColor;
        if (logoPath != null) this.logoPath = logoPath;
    }

    public void updateOperationalStatus(OperationalStatus status, String warningMessage) {
        if (status != null) {
            this.operationalStatus = status;
            if (status == OperationalStatus.OPEN) {
                this.warningMessage = null;
            } else if (warningMessage != null) {
                this.warningMessage = warningMessage;
            }
        }
    }

    public void configureScheduling(Integer appointmentBufferMinutes, ZoneId zoneId, Boolean autoConfirmationAppointment) {
        if (appointmentBufferMinutes != null) this.appointmentBufferMinutes = appointmentBufferMinutes;
        if (zoneId != null) this.zoneId = zoneId;
        if (autoConfirmationAppointment != null) this.autoConfirmationAppointment = autoConfirmationAppointment;
    }

    public void configureLoyaltyPriority(Boolean prioritize, Integer loyalWindowDays, Integer standardWindowDays) {
        if (prioritize != null) {
            this.isLoyalClientelePrioritized = prioritize;
            if (prioritize) {
                if (loyalWindowDays != null) this.loyalClientBookingWindowDays = loyalWindowDays;
                if (standardWindowDays != null) this.standardBookingWindow = standardWindowDays;
            }
        }
    }

    public void updateWhatsAppConnection(EvolutionConnectionState state, LocalDateTime lastResetAt, String pairingCode) {
        if (state != null) this.evolutionConnectionState = state;
        if (lastResetAt != null) this.whatsappLastResetAt = lastResetAt;
        if (pairingCode != null) this.lastPairingCode = pairingCode;
    }

    public void updateTenantStatus(TenantStatus status) {
        if (status != null) this.tenantStatus = status;
    }

    @Override
    public void prePersist() {
        super.prePersist();

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