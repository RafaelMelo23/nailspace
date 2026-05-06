package com.rafael.agendanails.webapp.application.user;

import com.rafael.agendanails.webapp.domain.email.EmailMessage;
import com.rafael.agendanails.webapp.domain.email.EmailNotifier;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.ResetPasswordDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.email.ForgotPasswordEmailDTO;
import com.rafael.agendanails.webapp.infrastructure.email.template.AuthEmailFactory;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetUseCaseTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthEmailFactory authEmailFactory;
    @Mock
    private EmailNotifier emailNotifier;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetUseCase passwordResetUseCase;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetUseCase, "domainUrl", "http://test.com");
    }

    @Test
    void shouldSendEmailWhenForgotPasswordIsRequested() {
        Client client = TestClientFactory.standard();
        when(userRepository.findByEmailIgnoreCase(client.getEmail())).thenReturn(Optional.of(client));
        when(jwtTokenService.generateResetPasswordToken(client.getId())).thenReturn("token123");
        
        EmailMessage emailMessage = EmailMessage.builder()
                .to(client.getEmail())
                .subject("Test")
                .body("Body")
                .build();
        when(authEmailFactory.createForgotPasswordEmail(any(ForgotPasswordEmailDTO.class))).thenReturn(emailMessage);

        passwordResetUseCase.forgotPasswordRequest(client.getEmail());

        verify(emailNotifier).send(emailMessage);
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotFoundDuringForgotPassword() {
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetUseCase.forgotPasswordRequest("wrong@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Usuário não encontrado");

        verify(emailNotifier, never()).send(any());
    }

    @Test
    void shouldUpdatePasswordWhenTokenIsValid() {
        ResetPasswordDTO dto = new ResetPasswordDTO("test@test.com", "newPassword123", "token123");
        when(passwordEncoder.encode(dto.newPassword())).thenReturn("encodedPassword");

        passwordResetUseCase.resetPassword(dto);

        verify(jwtTokenService).validateResetPasswordToken(dto);
        verify(userRepository).updatePassword(dto.userEmail(), "encodedPassword");
    }
}
