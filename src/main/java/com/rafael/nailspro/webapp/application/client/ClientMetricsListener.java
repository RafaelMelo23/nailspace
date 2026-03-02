package com.rafael.nailspro.webapp.application.client;

import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.ClientAuditMetrics;
import com.rafael.nailspro.webapp.domain.repository.ClientAuditMetricsRepository;
import com.rafael.nailspro.webapp.domain.repository.ClientRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentCancelledEvent;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentMissedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class ClientMetricsListener {

    private final ClientAuditMetricsRepository repository;
    private final ClientRepository clientRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFinished(AppointmentFinishedEvent event) {
        ClientAuditMetrics metrics = getAuditMetricsOrCreate(event.clientId(), event.tenantId());

        metrics.setTotalSpent(metrics.getTotalSpent().add(event.totalValue()));
        metrics.setLastVisitDate(event.completionDate());
        metrics.setCompletedAppointmentsCount(metrics.getCompletedAppointmentsCount() + 1);

        repository.save(metrics);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(AppointmentCancelledEvent event) {
        ClientAuditMetrics metrics = getAuditMetricsOrCreate(event.clientId(), event.tenantId());

        metrics.setCanceledAppointmentsCount(metrics.getCanceledAppointmentsCount() + 1);

        repository.save(metrics);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMissed(AppointmentMissedEvent event) {
        ClientAuditMetrics metrics = getAuditMetricsOrCreate(event.clientId(), event.tenantId());

        metrics.setMissedAppointmentsCount(metrics.getMissedAppointmentsCount() + 1);

        repository.save(metrics);
    }

    private ClientAuditMetrics getAuditMetricsOrCreate(Long clientId, String tenantId) {
        return repository.findByClient_Id(clientId)
                .orElseGet(() -> {
                    Client client = clientRepository.findById(clientId)
                            .orElseThrow(() -> new RuntimeException("Client not found"));
                    return new ClientAuditMetrics(tenantId, client);
                });
    }
}