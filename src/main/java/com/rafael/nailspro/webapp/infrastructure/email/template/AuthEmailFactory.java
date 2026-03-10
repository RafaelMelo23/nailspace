package com.rafael.nailspro.webapp.infrastructure.email.template;

import com.rafael.nailspro.webapp.domain.email.EmailMessage;
import com.rafael.nailspro.webapp.infrastructure.dto.email.ForgotPasswordEmailDTO;
import org.springframework.stereotype.Component;

@Component
public class AuthEmailFactory {

    //todo: decide name
    private static final String PLATFORM_NAME = "";

    public EmailMessage createForgotPasswordEmail(ForgotPasswordEmailDTO dto) {
        String subject = "Recuperação de senha - " + dto.tenantName();

        String body = """
            Olá!
            
            Você solicitou a redefinição de senha para sua conta no salão %s via %s.
            
            Clique no link abaixo para criar uma nova senha:
            %s
            
            Se você não solicitou esta alteração, pode ignorar este e-mail com segurança.
            """.formatted(dto.tenantName(), PLATFORM_NAME, dto.resetLink());

        return EmailMessage.builder()
                .to(dto.userEmail())
                .subject(subject)
                .body(body)
                .build();
    }
}