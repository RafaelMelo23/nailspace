package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.repository.ClientRepository;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.RegisterDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.UserAlreadyExistsException;
import com.rafael.agendanails.webapp.infrastructure.security.token.RefreshTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static final String DEFAULT_TENANT = "tenant-test";

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(DEFAULT_TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRegisterNewClientWhenDataIsValid() {
        RegisterDTO dto = new RegisterDTO("Test Name", "email@test.com", "password123", "11999999999");
        when(userRepository.findByEmailIgnoreCase(dto.email())).thenReturn(Optional.empty());
        when(clientRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.rawPassword())).thenReturn("encodedPassword");

        authenticationService.register(dto);

        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExistsDuringRegistration() {
        RegisterDTO dto = new RegisterDTO("Test Name", "email@test.com", "password123", "11999999999");
        when(userRepository.findByEmailIgnoreCase(dto.email())).thenReturn(Optional.of(TestClientFactory.standard()));

        assertThatThrownBy(() -> authenticationService.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("O E-mail informado já está sendo utilizado");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldThrowExceptionWhenPhoneNumberAlreadyExistsDuringRegistration() {
        RegisterDTO dto = new RegisterDTO("Test Name", "email@test.com", "password123", "11999999999");
        when(userRepository.findByEmailIgnoreCase(dto.email())).thenReturn(Optional.empty());
        when(clientRepository.findByPhoneNumber(anyString())).thenReturn(Optional.of(TestClientFactory.standard()));

        assertThatThrownBy(() -> authenticationService.register(dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("O telefone informado já está sendo utilizado");

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldRevokeTokenOnLogout() {
        authenticationService.logout("refresh-token", 1L);
        verify(refreshTokenService).revokeUserToken("refresh-token", 1L);
    }
}
