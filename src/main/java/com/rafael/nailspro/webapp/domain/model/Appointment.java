package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentConfirmedEvent;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@Setter(AccessLevel.PROTECTED)
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "appointment")
public class Appointment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "main_service_id", nullable = false)
    private SalonService mainSalonService;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue;

    @Column(name = "observations", length = 120)
    private String observations;

    @Setter(AccessLevel.PACKAGE)
    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status")
    private AppointmentStatus appointmentStatus;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "salon_trade_name")
    private String salonTradeName;

    @Column(name = "salon_zone_id")
    private ZoneId salonZoneId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appointment_addon_id")
    private List<AppointmentAddOn> addOns = new ArrayList<>();

    @OneToMany(mappedBy = "appointment", orphanRemoval = true)
    private List<WhatsappMessage> whatsappMessages = new ArrayList<>();

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    private void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    @DomainEvents
    public Collection<Object> domainEvents() {
        return domainEvents;
    }

    @AfterDomainEventPublication
    public void clearEvents() {
        domainEvents.clear();
    }

    @Override
    public void prePersist() {
        super.prePersist();
    }

    public void miss() {
        ensureStatusIsNotFinal();

        if (!hasEnded()) {
            throw new BusinessException("O agendamento ainda não terminou.");
        }

        this.setAppointmentStatus(AppointmentStatus.MISSED);
    }

    public void cancel() {
        ensureStatusIsNotFinal();
        ensureAppointmentIsActive();

        Instant now = Instant.now();

        this.setAppointmentStatus(AppointmentStatus.CANCELLED);

        Instant twoDaysFromNow = now.plus(2, ChronoUnit.DAYS);
        if (this.getStartDate().isBefore(twoDaysFromNow)) {
            this.client.incrementCancelledAppointmentCount();
        }
    }

    public void finish() {
        ensureStatusIsNotFinal();
        if (!hasEnded()) {
            throw new BusinessException("O agendamento ainda não terminou.");
        }
        this.setAppointmentStatus(AppointmentStatus.FINISHED);
    }

    public void confirm() {
        if (this.appointmentStatus == AppointmentStatus.CONFIRMED) {
            return;
        }
        ensureAppointmentIsActive();

        this.appointmentStatus = AppointmentStatus.CONFIRMED;
        registerEvent(new AppointmentConfirmedEvent(this.id));
    }

    public void ensureAppointmentIsActive() {
        if (hasEnded()) {
            throw new BusinessException("Este agendamento já terminou e não pode mais ser modificado.");
        }
    }

    public boolean hasEnded() {
        return Instant.now().isAfter(this.endDate);
    }

    public void ensureStatusIsNotFinal() {
        if (this.appointmentStatus == AppointmentStatus.FINISHED ||
                this.appointmentStatus == AppointmentStatus.MISSED ||
                this.appointmentStatus == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Este agendamento já foi finalizado.");
        }
    }

    public BigDecimal calculateTotalValue() {
        if (mainSalonService == null || mainSalonService.getValue() == null) {
            throw new IllegalStateException("Service or value cannot be null.");
        }
        BigDecimal mainValue = BigDecimal.valueOf(this.mainSalonService.getValue());

        BigDecimal addOnsValue = this.addOns.stream().map(this::calculateAddonValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return mainValue.add(addOnsValue);
    }

    private BigDecimal calculateAddonValue(AppointmentAddOn addon) {
        if (addon.getService() == null || addon.getService().getValue() == null) {
            throw new IllegalStateException("Service addon or value cannot be null.");
        }

        return BigDecimal.valueOf(addon.getService().getValue())
                .multiply(BigDecimal.valueOf(addon.getQuantity()));
    }

    public static Appointment create(
            AppointmentCreateDTO dto,
            Client client,
            Professional professional,
            SalonService mainService,
            List<AppointmentAddOn> addOns,
            SalonProfile salonProfile,
            TimeInterval interval
    ) {

        if (dto == null || client == null || professional == null ||
                mainService == null || salonProfile == null || interval == null) {
            throw new IllegalArgumentException("Appointment creation arguments cannot be null.");
        }

        List<AppointmentAddOn> safeAddOns =
                addOns == null ? new ArrayList<>() : new ArrayList<>(addOns);

        Appointment appointment = Appointment.builder()
                .appointmentStatus(AppointmentStatus.PENDING)
                .startDate(interval.realTimeStart())
                .endDate(interval.realTimeEnd())
                .client(client)
                .professional(professional)
                .mainSalonService(mainService)
                .addOns(safeAddOns)
                .observations(dto.observation().orElse(null))
                .salonTradeName(salonProfile.getTradeName())
                .salonZoneId(salonProfile.getZoneId())
                .build();

        appointment.setTotalValue(appointment.calculateTotalValue());

        if (salonProfile.isAutoConfirmationAppointment()) {
            appointment.confirm();
        }

        return appointment;
    }

    public static TimeInterval calculateIntervalAndBuffer(
            AppointmentCreateDTO dto,
            long durationInSeconds,
            SalonProfile salonProfile
    ) {

        Instant start = dto.zonedAppointmentDateTime().toInstant();
        Instant realEnd = start.plusSeconds(durationInSeconds);
        Instant endWithBuffer = realEnd.plus(
                salonProfile.getAppointmentBufferMinutes(),
                ChronoUnit.MINUTES
        );

        return TimeInterval.builder()
                .realTimeStart(start)
                .realTimeEnd(realEnd)
                .endTimeWithBuffer(endWithBuffer)
                .salonZoneId(salonProfile.getZoneId())
                .build();
    }

    public static long calculateDurationInSeconds(
            SalonService mainService,
            List<AppointmentAddOn> addOns
    ) {
        if (mainService == null) throw new IllegalArgumentException("Main service cannot be null.");

        long mainDuration = mainService.getDurationInSeconds();

        long addOnsDuration = (addOns == null ? List.<AppointmentAddOn>of() : addOns)
                .stream()
                .filter(addon -> addon != null && addon.getService() != null)
                .mapToLong(addon -> addon.getService().getDurationInSeconds())
                .sum();

        return mainDuration + addOnsDuration;
    }
}
