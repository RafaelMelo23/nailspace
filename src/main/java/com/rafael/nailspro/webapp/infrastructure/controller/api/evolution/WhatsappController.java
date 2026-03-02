package com.rafael.nailspro.webapp.infrastructure.controller.api.evolution;

import com.rafael.nailspro.webapp.application.whatsapp.ManageWhatsappConnectionUseCase;
import com.rafael.nailspro.webapp.domain.enums.evolution.WhatsappConnectionMethod;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/whatsapp")
@PreAuthorize("hasRole('ADMIN')")
public class WhatsappController {

    private final ManageWhatsappConnectionUseCase whatsappConnectionUseCase;

    @PostMapping
    public ResponseEntity<Void> setupConnection(@RequestParam @NotNull(message = "O método de conexão é obrigatório")
                                                WhatsappConnectionMethod connectionMethod) {

        whatsappConnectionUseCase.setupInitialConnection(TenantContext.getTenant(), connectionMethod);
        return ResponseEntity.ok().build();
    }
}