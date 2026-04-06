package com.rafael.agendanails.webapp.infrastructure.controller.api.evolution.webhook;

import com.rafael.agendanails.webapp.application.whatsapp.webhook.WebhookProcessorService;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.webhook.EvolutionWebhookResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
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
        log.info("Received webhook: event={}, instance={}", dto.event(), dto.instance());
        webhookProcessorService.handleWebhook(dto);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONNECTION, "close")
                .build();
    }
}
