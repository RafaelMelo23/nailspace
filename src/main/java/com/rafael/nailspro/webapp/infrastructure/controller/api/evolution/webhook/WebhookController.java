package com.rafael.nailspro.webapp.infrastructure.controller.api.evolution.webhook;

import com.rafael.nailspro.webapp.application.whatsapp.webhook.WebhookProcessorService;
import com.rafael.nailspro.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "Evolution WhatsApp webhooks")
public class WebhookController {

    private final WebhookProcessorService webhookProcessorService;

    @Operation(summary = "Process webhook", description = "Processes an incoming Evolution webhook payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed"),
            @ApiResponse(responseCode = "400", description = "Invalid payload")
    })
    @PostMapping
    public ResponseEntity<Void> process(@RequestBody EvolutionWebhookResponseDTO<?> dto) {

        webhookProcessorService.handleWebhook(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONNECTION, "close")
                .build();
    }
}
