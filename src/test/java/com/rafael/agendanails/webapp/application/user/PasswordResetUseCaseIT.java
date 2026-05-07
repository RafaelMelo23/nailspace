package com.rafael.agendanails.webapp.application.user;

import com.rafael.agendanails.webapp.domain.email.EmailNotifier;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import com.rafael.agendanails.webapp.support.factory.TestUserPrincipalFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class PasswordResetUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private PasswordResetUseCase passwordResetUseCase;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailNotifier emailNotifier;

    @Test
    void shouldUpdatePasswordInDatabaseWhenResetFlowIsCompleted() {
        Client client = TestClientFactory.standardForIt();
        client.setPassword(passwordEncoder.encode("oldPassword123"));
        userRepository.save(client);

        String resetToken = jwtTokenService.generateResetPasswordToken(client.getId());
        ResetPasswordDTO dto = new ResetPasswordDTO(client.getEmail(), "newSecurePassword123", resetToken);

        passwordResetUseCase.resetPassword(dto);

        Optional<User> updatedUser = userRepository.findByEmailIgnoreCase(client.getEmail());
        assertThat(updatedUser).isPresent();
        assertThat(passwordEncoder.matches("newSecurePassword123", updatedUser.get().getPassword())).isTrue();
    }

    @Test
    void shouldSendEmailWhenForgotPasswordIsRequestedIT() {
        Client client = TestClientFactory.standardForIt();
        userRepository.save(client);

        passwordResetUseCase.forgotPasswordRequest(client.getEmail());

        verify(emailNotifier).send(any());
    }

    @Test
    void shouldThrowExceptionWhenTokenIsInvalidDuringReset() {
        Client client = TestClientFactory.standardForIt();
        userRepository.save(client);

        String wrongToken = jwtTokenService.generateAuthToken(TestUserPrincipalFactory.from(client));
        ResetPasswordDTO dto = new ResetPasswordDTO(client.getEmail(), "newPassword123", wrongToken);

        assertThatThrownBy(() -> passwordResetUseCase.resetPassword(dto))
                .isInstanceOf(RuntimeException.class);
    }
}

