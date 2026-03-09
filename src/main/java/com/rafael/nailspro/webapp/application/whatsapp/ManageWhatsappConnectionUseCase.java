package com.rafael.nailspro.webapp.application.whatsapp;

import com.rafael.nailspro.webapp.application.salon.business.SalonProfileService;
import com.rafael.nailspro.webapp.domain.enums.evolution.WhatsappConnectionMethod;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.domain.whatsapp.WhatsappProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageWhatsappConnectionUseCase {

    private final SalonProfileService salonService;
    private final WhatsappProvider whatsappProvider;

    @Transactional
    public void setupInitialConnection(String tenantId, WhatsappConnectionMethod connectionMethod) {
        log.info("Starting WhatsApp initial connection setup for tenantId={}", tenantId);
        deleteInstance(tenantId);
        SalonProfile salon = salonService.findWithOwnerByTenantId(tenantId);

        createInstance(tenantId);
        updateConnectionInfo(tenantId, salon);

        connectViaChosenMethod(tenantId, connectionMethod, salon.getComercialPhone());

        log.info("WhatsApp initial connection setup completed for tenantId={}", tenantId);
    }

    private void createInstance(String tenantId) {
        log.info("Creating new WhatsApp instance for tenantId={}", tenantId);
        whatsappProvider.createInstance(tenantId);
    }

    private void connectViaChosenMethod(String tenantId, WhatsappConnectionMethod connectionMethod, String phoneNumber) {
        switch (connectionMethod) {
            case PAIRING_CODE -> {
                log.info("Connecting WhatsApp instance using PAIRING_CODE for tenantId={}", tenantId);
                whatsappProvider.instanceConnect(tenantId, phoneNumber);
            } case QR_CODE -> {
                log.info("Connecting WhatsApp instance using QR_CODE for tenantId={}", tenantId);
                whatsappProvider.instanceConnect(tenantId, null);
            }
        }
    }

    private void updateConnectionInfo(String tenantId, SalonProfile salon) {
        log.debug("Salon profile loaded for tenantId={}", tenantId);
        salon.setWhatsappLastResetAt(LocalDateTime.now());
        salonService.save(salon);
        log.debug("Salon profile updated with whatsappLastResetAt for tenantId={}", tenantId);
    }

    private void deleteInstance(String tenantId) {
        try {
            log.debug("Attempting to delete existing WhatsApp instance for tenantId={}", tenantId);
            whatsappProvider.deleteInstance(tenantId);
            log.debug("Previous WhatsApp instance deleted for tenantId={}", tenantId);
        } catch (Exception ex) {
            log.warn("Failed to delete existing WhatsApp instance for tenantId={}. Continuing setup. Error={}",
                    tenantId, ex.getMessage());
        }
    }
}