package com.rafael.agendanails.webapp.application.user;

import com.rafael.agendanails.webapp.domain.email.EmailMessage;
import com.rafael.agendanails.webapp.domain.email.EmailNotifier;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.email.ForgotPasswordEmailDTO;
import com.rafael.agendanails.webapp.infrastructure.email.template.AuthEmailFactory;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordResetUseCase {

    private final UserRepository userRepository;
    private final AuthEmailFactory authEmailFactory;
    private final EmailNotifier emailNotifier;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    @Value("${domain.url}")
    private String domainUrl;

    public void forgotPasswordRequest(String userEmail) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        String resetToken = jwtTokenService.generateResetPasswordToken(user.getId());
        String passwordResetLink = buildResetPasswordUrl(resetToken);

        ForgotPasswordEmailDTO emailDTO = buildForgotPasswordEmailDTO(
                userEmail,
                passwordResetLink,
                TenantContext.getTenant());
        sendForgotPasswordEmail(emailDTO);
    }

    private String buildResetPasswordUrl(String resetToken) {
        return UriComponentsBuilder.fromUriString(domainUrl)
                .pathSegment("redefinir-senha")
                .queryParam("resetToken", resetToken)
                .build()
                .toUriString();
    }

    private ForgotPasswordEmailDTO buildForgotPasswordEmailDTO(String userEmail, String resetLink, String tenantName) {
        return ForgotPasswordEmailDTO.builder()
                .userEmail(userEmail)
                .resetLink(resetLink)
                .tenantName(tenantName)
                .build();
    }

    private void sendForgotPasswordEmail(ForgotPasswordEmailDTO emailDTO) {
        var forgotPasswordEmail = authEmailFactory.createForgotPasswordEmail(emailDTO);
        emailNotifier.send(EmailMessage.builder()
                .to(forgotPasswordEmail.to())
                .subject(forgotPasswordEmail.subject())
                .body(forgotPasswordEmail.body())
                .build());
    }

    @Transactional
    public void resetPassword(ResetPasswordDTO resetPasswordDTO) {
        jwtTokenService.validateResetPasswordToken(resetPasswordDTO);

        userRepository.updatePassword(
                resetPasswordDTO.userEmail(),
                passwordEncoder.encode(resetPasswordDTO.newPassword()));
    }
}
