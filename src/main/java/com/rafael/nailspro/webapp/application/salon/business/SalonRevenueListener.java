package com.rafael.nailspro.webapp.application.salon.business;

import com.rafael.nailspro.webapp.domain.model.SalonDailyRevenue;
import com.rafael.nailspro.webapp.domain.repository.SalonDailyRevenueRepository;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.event.AppointmentFinishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SalonRevenueListener {

    private final SalonDailyRevenueRepository repository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFinished(AppointmentFinishedEvent event) {
        LocalDate date = event.completionDate().toLocalDate();

        var revenue = repository.findByTenantIdAndDate(event.tenantId(), date)
                .orElse(new SalonDailyRevenue(event.tenantId(), date));

        revenue.setTotalRevenue(revenue.getTotalRevenue().add(event.totalValue()));
        revenue.setAppointmentsCount(revenue.getAppointmentsCount() + 1);

        repository.save(revenue);
    }
}
