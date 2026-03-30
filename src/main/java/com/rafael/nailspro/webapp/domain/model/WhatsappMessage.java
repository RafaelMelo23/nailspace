package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageStatus;
import com.rafael.nailspro.webapp.domain.enums.whatsapp.WhatsappMessageType;
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
@Table(name = "whatsapp_message")
public class WhatsappMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retention_forecast_id")
    private RetentionForecast retentionForecast;

    @Enumerated(EnumType.STRING)
    @Column(name = "whatsapp_message_type", nullable = false)
    private WhatsappMessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "whatsapp_message_status", nullable = false)
    private WhatsappMessageStatus messageStatus;

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