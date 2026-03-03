package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.RetentionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "retention_forecast")
@Filter(name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_service_id", nullable = false)
    private SalonService lastService;

    @Column(name = "predicted_return_date", nullable = false)
    private Instant predictedReturnDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RetentionStatus status;

    @OneToOne
    @JoinColumn(name = "origin_appointment_id")
    private Appointment originAppointment;

    public static RetentionForecast create(Appointment appointment,
                                           SalonService service) {

        Instant predictedReturn = predictReturnDate(
                appointment.getEndDate(),
                service.getMaintenanceIntervalDays(),
                appointment.getSalonZoneId()
        );

        return RetentionForecast.builder()
                .lastService(service)
                .client(appointment.getClient())
                .status(RetentionStatus.PENDING)
                .originAppointment(appointment)
                .predictedReturnDate(predictedReturn)
                .professional(appointment.getProfessional())
                .build();
    }

    public static Instant predictReturnDate(Instant appointmentEnd,
                                     int expectedMaintenanceDays,
                                     ZoneId salonZone) {

        return Instant
                .from(ZonedDateTime.ofInstant(appointmentEnd, salonZone)
                        .plusDays(expectedMaintenanceDays));
    }
}