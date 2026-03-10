package com.rafael.nailspro.webapp.infrastructure.email;

import com.rafael.nailspro.webapp.domain.email.EmailQuotaManager;
import com.rafael.nailspro.webapp.domain.model.EmailUsageQuota;
import com.rafael.nailspro.webapp.domain.repository.EmailQuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JpaEmailQuotaManager implements EmailQuotaManager {

    private final EmailQuotaRepository emailQuotaRepository;
    private static final int MONTH_MAX_QUOTA = 3000;
    private static final int DAY_MAX_QUOTA = 100;

    @Override
    @Transactional
    public void registerSuccessfulSend() {
        LocalDate today = LocalDate.now();

        getQuota(today).ifPresentOrElse(
                EmailUsageQuota::increment,
                () -> emailQuotaRepository.save(
                        EmailUsageQuota.builder()
                                .dailyCount(1)
                                .usageDate(today)
                                .build())
        );
    }

    @Override
    public boolean isQuotaAvailable() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        if (isMonthlyQuotaExceeded(thirtyDaysAgo, today)
                || isDailyQuotaExceeded(today)) {
            return false;
        }

        return true;
    }

    private boolean isMonthlyQuotaExceeded(LocalDate thirtyDaysAgo, LocalDate today) {
        Integer monthlyUsageCount = emailQuotaRepository.getMonthlyUsageCount(thirtyDaysAgo, today)
                .orElse(0);

        return monthlyUsageCount >= MONTH_MAX_QUOTA;
    }

    private boolean isDailyQuotaExceeded(LocalDate today) {
        Integer dailyUsageCount = emailQuotaRepository.getDailyUsageCount(today)
                .orElse(0);

        return dailyUsageCount >= DAY_MAX_QUOTA;
    }

    private Optional<EmailUsageQuota> getQuota(LocalDate day) {
        return emailQuotaRepository.findByUsageDate(day);
    }
}