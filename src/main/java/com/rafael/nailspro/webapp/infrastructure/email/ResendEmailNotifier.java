package com.rafael.nailspro.webapp.infrastructure.email;

import com.rafael.nailspro.webapp.domain.email.EmailMessage;
import com.rafael.nailspro.webapp.domain.email.EmailNotifier;
import com.rafael.nailspro.webapp.domain.email.EmailQuotaManager;
import com.rafael.nailspro.webapp.infrastructure.dto.email.ResendEmailBodyDTO;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResendEmailNotifier implements EmailNotifier {

    @Value("${resend.email.address}")
    private String resendEmailAddress;
    @Value("${resend.api.key}")
    private String resendApiKey;

    private final EmailQuotaManager emailQuotaManager;
    private final RestClient restClient;

    @Override
    public void send(EmailMessage emailMessage) {
        validateEmailingQuota();

        restClient.post()
                .uri("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .header("User-Agent", "my-app/1.0")
                .body(buildEmailBody(emailMessage))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new RuntimeException("Resend API Error: " + response.getStatusText());
                })
                .toBodilessEntity();

        emailQuotaManager.registerSuccessfulSend();
    }

    private void validateEmailingQuota() {
        if (!emailQuotaManager.isQuotaAvailable()) {
            throw new BusinessException("Não foi possível enviar o email.");
        }
    }

    private ResendEmailBodyDTO buildEmailBody(EmailMessage emailMessage) {
        return ResendEmailBodyDTO.builder()
                .from(resendEmailAddress)
                .html(emailMessage.body())
                .to(List.of(emailMessage.to()))
                .subject(emailMessage.subject())
                .build();
    }
}