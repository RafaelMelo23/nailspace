package com.rafael.agendanails.webapp.infrastructure.whatsapp.evolution;

import com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionWebhookEvent;
import com.rafael.agendanails.webapp.domain.whatsapp.SentMessageResult;
import com.rafael.agendanails.webapp.domain.whatsapp.WhatsappProvider;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.instance.CreateInstanceRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.instance.WebhookDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.text.SendTextRequestDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.whatsapp.evolution.text.SendTextResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.helper.PhoneNumberHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static com.rafael.agendanails.webapp.domain.enums.evolution.EvolutionIntegraton.WHATSAPP_BAILEYS;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "evolution", matchIfMissing = true)
public class EvolutionWhatsappService implements WhatsappProvider {

    private static final String INSTANCE_PATH = "/instance";
    private static final String MESSAGE_PATH = "/message";

    @Qualifier("evolutionRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${evolution.url}")
    private String evolutionApiBaseUrl;

    @Value("${evolution.apikey:}")
    private String evolutionApiKey;

    @Value("${evolution.webhook.url}")
    private String webhookUrl;

    @Override
    public void createInstance(String tenantId) {
        String url = buildInstanceUrl("/create");
        log.info("Evolution API - CREATE instance request. tenantId={}", tenantId);

        HttpEntity<CreateInstanceRequestDTO> request =
                new HttpEntity<>(buildCreateInstancePayload(tenantId), getHeaders());
        try {
            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Unexpected response status: " + response.getStatusCode());
            }
            log.debug("Evolution API - CREATE success. tenantId={}", tenantId);
        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Evolution API - Instance already exists. tenantId={}", tenantId);
        } catch (Exception ex) {
            log.error("Evolution API - CREATE failed. tenantId={}", tenantId, ex);
            throw ex;
        }
    }

    @Override
    public void instanceConnect(String instanceName, String phoneNumber) {
        String url = buildConnectUrl(instanceName, phoneNumber);
        log.info("Evolution API - CONNECT request. instanceName={}", instanceName);

        HttpEntity<Void> request = new HttpEntity<>(getHeaders());
        try {
            ResponseEntity<Void> response =
                    restTemplate.exchange(url, HttpMethod.GET, request, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Unexpected response status: " + response.getStatusCode());
            }
            log.debug("Evolution API - CONNECT success. instanceName={}", instanceName);
        } catch (Exception ex) {
            log.error("Evolution API - CONNECT failed. instanceName={}", instanceName, ex);
            throw ex;
        }
    }

    @Override
    public void deleteInstance(String instanceName) {
        String url = buildInstanceUrl("/delete/" + instanceName);
        log.info("Evolution API - DELETE request. instanceName={}", instanceName);

        HttpEntity<Void> request = new HttpEntity<>(getHeaders());

        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.debug("Evolution API - DELETE success. instanceName={}", instanceName);

            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Evolution API - DELETE interrupted. instanceName={}", instanceName, ex);
            throw new IllegalStateException("Deletion interrupted", ex);

        } catch (Exception ex) {
            log.error("Evolution API - DELETE failed. instanceName={}", instanceName, ex);
            throw ex;
        }
    }

    @Override
    public void logout(String instanceName) {
        String url = buildInstanceUrl("/logout/" + instanceName);
        log.info("Evolution API - LOGOUT request. instanceName={}", instanceName);

        HttpEntity<Void> request = new HttpEntity<>(getHeaders());
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
            log.debug("Evolution API - LOGOUT success. instanceName={}", instanceName);

        } catch (Exception ex) {
            log.error("Evolution API - LOGOUT failed. instanceName={}", instanceName, ex);
            throw ex;
        }
    }

    @Override
    public SentMessageResult sendText(String instanceName, String message, String targetNumber) {
        String url = buildMessageUrl("/sendText/" + instanceName);
        applyRandomDelay();
        String formattedNumber = PhoneNumberHelper.formatPhoneNumber(targetNumber);
        log.info("Evolution API - SEND TEXT request. instanceName={} targetEnding={}", instanceName, maskNumber(formattedNumber));

        SendTextRequestDTO bodyDTO = SendTextRequestDTO.of(formattedNumber, message);
        HttpEntity<SendTextRequestDTO> request = new HttpEntity<>(bodyDTO, getHeaders());
        try {
            log.debug("Evolution API - SENDING TEXT. instanceName={}", instanceName);
            SendTextResponseDTO dto = restTemplate.postForObject(url, request, SendTextResponseDTO.class);

            if (dto != null) return buildFromResponse(dto);
        } catch (Exception ex) {
            log.error("Evolution API - SEND TEXT failed. instanceName={}", instanceName, ex);
            throw ex;
        }
        throw new IllegalStateException("Failed to send message");
    }

    private void applyRandomDelay() {
        final int minMillis = 850, maxMillis = 4000;
        try {
            Thread.sleep((long) (Math.random() * (maxMillis - minMillis + 1)) + minMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Random delay interrupted", ex);
        }
    }

    private SentMessageResult buildFromResponse(SendTextResponseDTO dto) {

        return SentMessageResult.builder()
                .messageId(dto.messageId())
                .status(dto.status())
                .build();
    }

    private CreateInstanceRequestDTO buildCreateInstancePayload(String tenantId) {
        List<EvolutionWebhookEvent> events = List.of(
                EvolutionWebhookEvent.QRCODE_UPDATED,
                EvolutionWebhookEvent.SEND_MESSAGE,
                EvolutionWebhookEvent.CONNECTION_UPDATE,
                EvolutionWebhookEvent.MESSAGES_UPDATE
        );

        log.info("Evolution API - Setting up webhook URL for instance {}: {}", tenantId, webhookUrl);

        WebhookDTO webhook = WebhookDTO.builder()
                .enabled(true)
                .base64(false)
                .byEvents(false)
                .url(webhookUrl)
                .events(events)
                .headers(Map.of("apiKey", evolutionApiKey))
                .build();

        return CreateInstanceRequestDTO.builder()
                .instanceName(tenantId)
                .webhook(webhook)
                .qrcode(false)
                .integration(WHATSAPP_BAILEYS)
                .build();
    }

    private String buildInstanceUrl(String path) {
        return UriComponentsBuilder
                .fromUriString(evolutionApiBaseUrl)
                .path(INSTANCE_PATH)
                .path(path)
                .toUriString();
    }

    private String buildMessageUrl(String path) {
        return UriComponentsBuilder
                .fromUriString(evolutionApiBaseUrl)
                .path(MESSAGE_PATH)
                .path(path)
                .toUriString();
    }

    private String buildConnectUrl(String instanceName, String phoneNumber) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(evolutionApiBaseUrl)
                .pathSegment("instance", "connect", instanceName);

        if (phoneNumber != null) {
            builder.queryParam("phoneNumber", phoneNumber);
        }

        return builder.toUriString();
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", evolutionApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String maskNumber(String number) {
        if (number.length() <= 4) return "****";
        return "****" + number.substring(number.length() - 4);
    }
}