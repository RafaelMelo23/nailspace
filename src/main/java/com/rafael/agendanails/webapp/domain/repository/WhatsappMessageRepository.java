package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageStatus;
import com.rafael.agendanails.webapp.domain.enums.whatsapp.WhatsappMessageType;
import com.rafael.agendanails.webapp.domain.model.WhatsappMessage;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WhatsappMessageRepository extends JpaRepository<WhatsappMessage, Long> {

    @IgnoreTenantFilter
    @Modifying
    @Transactional
    @Query("""
    DELETE FROM WhatsappMessage wm
    WHERE (wm.sentAt IS NOT NULL AND wm.sentAt < :instant)
       OR (wm.sentAt IS NULL AND wm.lastAttemptAt IS NOT NULL AND wm.lastAttemptAt < :instant)
    """)
    int deleteOldMessagesInBatch(@Param("instant") Instant instant);

    @IgnoreTenantFilter
    @Query("""
    SELECT wm.appointment.id FROM WhatsappMessage wm
    WHERE wm.attempts <= :maxRetries
    AND wm.messageStatus = :status
    AND wm.messageType = :type
    AND wm.appointment IS NOT NULL""")
    List<Long> findRetriableAppointmentIds(@Param("maxRetries") int maxRetries,
                                           @Param("status") WhatsappMessageStatus status,
                                           @Param("type") WhatsappMessageType type);

    @IgnoreTenantFilter
    @Query("""
    SELECT wm.retentionForecast.id FROM WhatsappMessage wm
    WHERE wm.attempts <= :maxRetries
    AND wm.messageStatus = :status
    AND wm.messageType = :type
    AND wm.retentionForecast IS NOT NULL""")
    List<Long> findRetriableRetentionForecastIds(@Param("maxRetries") int maxRetries,
                                                 @Param("status") WhatsappMessageStatus status,
                                                 @Param("type") WhatsappMessageType type);

    Optional<WhatsappMessage> findByAppointmentIdAndMessageType(Long appointmentId, WhatsappMessageType type);

    Optional<WhatsappMessage> findByRetentionForecastIdAndMessageType(Long retentionForecastId, WhatsappMessageType type);

    @IgnoreTenantFilter
    Optional<WhatsappMessage> findByExternalMessageId(String externalMessageId);
}
