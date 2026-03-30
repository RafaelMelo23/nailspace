package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "retention_forecast")
public class RetentionForecast extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    private Professional professional;

    @Column(name = "predicted_return_date", nullable = false)
    private Instant predictedReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RetentionStatus status;

    @OneToOne
    @JoinColumn(name = "origin_appointment_id")
    private Appointment originAppointment;

    @ManyToMany
    @JoinTable(name = "retention_forecast_salonServices",
            joinColumns = @JoinColumn(name = "retentionForecast_id"),
            inverseJoinColumns = @JoinColumn(name = "salonServices_id"))
    private List<SalonService> salonServices = new ArrayList<>();

    public static RetentionForecast create(Appointment appointment) {
        List<SalonService> allServices = getServicesWithMaintenanceInterval(appointment);
        if (allServices.isEmpty()) throw new IllegalArgumentException("Cannot create an forecast without valid services");

        int shortestMaintenanceInterval = getShortestMaintenanceInterval(allServices);

        Instant predictedReturn = predictReturnDate(
                appointment.getEndDate(),
                shortestMaintenanceInterval,
                appointment.getSalonZoneId()
        );

        return RetentionForecast.builder()
                .salonServices(allServices)
                .client(appointment.getClient())
                .status(RetentionStatus.PENDING)
                .originAppointment(appointment)
                .predictedReturnDate(predictedReturn)
                .professional(appointment.getProfessional())
                .tenantId(appointment.getTenantId())
                .build();
    }

    private static List<SalonService> getServicesWithMaintenanceInterval(Appointment appointment) {
        return Stream.concat(
                        Stream.of(appointment.getMainSalonService()),
                        appointment.getAddOns().stream()
                                .map(AppointmentAddOn::getService)
                )
                .filter(Objects::nonNull)
                .filter(salonService -> salonService.getMaintenanceIntervalDays() != null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static int getShortestMaintenanceInterval(List<SalonService> allServices) {
        return allServices.stream()
                .mapToInt(SalonService::getMaintenanceIntervalDays)
                .min()
                .orElse(15);
    }

    public static Instant predictReturnDate(Instant appointmentEnd,
                                            int expectedMaintenanceDays,
                                            ZoneId salonZone) {
        return Instant
                .from(ZonedDateTime.ofInstant(appointmentEnd, salonZone)
                        .plusDays(expectedMaintenanceDays));
    }
}