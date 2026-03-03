package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationStatus;
import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentNotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Setter
@Getter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointment_notification")
public class AppointmentNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_notification_type", nullable = false)
    private AppointmentNotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_notification_status", nullable = false)
    private AppointmentNotificationStatus notificationStatus;

    @Column(name = "external_message_id")
    private String externalMessageId;

    @Column(name = "destination_number")
    private String destinationNumber;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "sent_at")
    private Instant sentAt;

}