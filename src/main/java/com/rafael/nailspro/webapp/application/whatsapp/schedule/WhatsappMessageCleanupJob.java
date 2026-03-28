package com.rafael.nailspro.webapp.application.whatsapp.schedule;

import com.rafael.nailspro.webapp.domain.repository.WhatsappMessageRepository;
import com.rafael.nailspro.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Log4j2
@Service
@RequiredArgsConstructor
public class WhatsappMessageCleanupJob {

    private final WhatsappMessageRepository messageRepository;

    @IgnoreTenantFilter
    @Scheduled(cron = "0 0 5 * * *")
    public void deleteOldMessages() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        try {
            int deleted = messageRepository.deleteOldMessagesInBatch(cutoff);
            if (deleted > 0) {
                log.info(
                        "Deleted {} whatsapp messages older than {}",
                        deleted,
                        cutoff
                );
            }
        } catch (Exception ex) {
            log.error("Failed to cleanup old whatsapp messages", ex);
        }
    }
}
