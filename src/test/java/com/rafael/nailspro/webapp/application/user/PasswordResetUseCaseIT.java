package com.rafael.nailspro.webapp.application.user;

import com.rafael.nailspro.webapp.domain.email.EmailNotifier;
import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.User;
import com.rafael.nailspro.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
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
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailNotifier emailNotifier;

    @Test
    void shouldUpdatePasswordInDatabaseWhenResetFlowIsCompleted() {
        Client client = TestClientFactory.standardForIt();
        client.setPassword(passwordEncoder.encode("oldPassword123"));
        userRepository.save(client);

        String resetToken = tokenService.generateResetPasswordToken(client.getId());
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

        String wrongToken = tokenService.generateAuthToken(client);
        ResetPasswordDTO dto = new ResetPasswordDTO(client.getEmail(), "newPassword123", wrongToken);

        assertThatThrownBy(() -> passwordResetUseCase.resetPassword(dto))
                .isInstanceOf(RuntimeException.class);
    }
}
