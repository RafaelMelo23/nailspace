package com.rafael.agendanails.webapp.application.whatsapp.connection;

import com.rafael.agendanails.webapp.application.salon.business.SalonProfileService;
import com.rafael.agendanails.webapp.domain.enums.evolution.WhatsappConnectionMethod;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.WhatsappConnectionResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.rafael.agendanails.webapp.infrastructure.helper.PhoneNumberHelper.formatPhoneNumber;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageWhatsappConnectionUseCase {

    private final SalonProfileService salonService;
    private final WhatsappProvider whatsappProvider;

    @Transactional
    public WhatsappConnectionResponseDTO setupInitialConnection(String tenantId, WhatsappConnectionMethod connectionMethod) {
        log.info("Starting WhatsApp initial connection setup for tenantId={}, method={}", tenantId, connectionMethod);
        String pairingCode = switch (connectionMethod) {
            case PAIRING_CODE -> setupPairingCodeConnection(tenantId);
            case QR_CODE -> {
                setupQrCodeConnection(tenantId);
                yield null;
            }
        };
        return WhatsappConnectionResponseDTO.of(connectionMethod, pairingCode);
    }

    @Transactional
    public String setupPairingCodeConnection(String tenantId) {
        log.info("Setting up WhatsApp PAIRING_CODE connection for tenantId={}", tenantId);
        
        salonService.resetWhatsappConnectionState(tenantId);
        SalonProfile salon = salonService.findWithOwnerByTenantId(tenantId);
        
        deleteInstance(tenantId);
        
        String phoneNumber = salon.getComercialPhone();

        if (phoneNumber == null) {
            throw new BusinessException("É necessário ter registrado um número comercial para conectar-se ao whatsapp");
        } else {
            phoneNumber = formatPhoneNumber(phoneNumber);
        }
        
        log.info("Creating instance with phoneNumber for PAIRING_CODE flow. tenantId={}", tenantId);
        whatsappProvider.createInstance(tenantId, phoneNumber);
        
        String pairingCode = whatsappProvider.instanceConnect(tenantId, phoneNumber);
        
        log.info("WhatsApp PAIRING_CODE connection setup completed for tenantId={}. PairingCode generated.", tenantId);
        return pairingCode;
    }

    @Transactional
    public void setupQrCodeConnection(String tenantId) {
        log.info("Setting up WhatsApp QR_CODE connection for tenantId={}", tenantId);
        
        SalonProfile salon = salonService.findWithOwnerByTenantId(tenantId);
        updateConnectionInfo(tenantId, salon);
        
        deleteInstance(tenantId);
        
        log.info("Creating instance without phoneNumber for QR_CODE flow. tenantId={}", tenantId);
        whatsappProvider.createInstance(tenantId, null);

        whatsappProvider.instanceConnect(tenantId, null);
        
        log.info("WhatsApp QR_CODE connection setup completed for tenantId={}", tenantId);
    }

    private void updateConnectionInfo(String tenantId, SalonProfile salon) {
        log.debug("Updating connection info for tenantId={}", tenantId);
        salon.updateWhatsAppConnection(null, LocalDateTime.now(), null);
        salonService.save(salon);
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
