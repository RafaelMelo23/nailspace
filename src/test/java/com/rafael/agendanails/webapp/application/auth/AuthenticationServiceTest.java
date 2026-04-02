package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.repository.ClientRepository;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.AuthResultDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.LoginDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.RegisterDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.TokenRefreshResponseDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.exception.LoginException;
import com.rafael.agendanails.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.agendanails.webapp.infrastructure.exception.UserAlreadyExistsException;
import com.rafael.agendanails.webapp.infrastructure.security.token.TokenService;
import com.rafael.agendanails.webapp.infrastructure.security.token.refresh.RefreshTokenService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    private TokenService tokenService;
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
    void shouldLoginSuccessfullyAndReturnTokensWhenCredentialsAreValid() {
        LoginDTO loginDTO = new LoginDTO("email@test.com", "password");
        Client user = TestClientFactory.builder()
                .email(loginDTO.email())
                .password("encodedPassword")
                .tenantId(DEFAULT_TENANT)
                .build();

        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.password(), user.getPassword())).thenReturn(true);
        when(tokenService.generateAuthToken(user)).thenReturn("jwt-token");
        
        RefreshToken refreshToken = RefreshToken.builder().token("refresh-token").build();
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResultDTO result = authenticationService.login(loginDTO);

        assertThat(result.jwtToken()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundDuringLogin() {
        LoginDTO loginDTO = new LoginDTO("email@test.com", "password");
        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Os dados informados são inválidos");

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void shouldThrowExceptionWhenPasswordDoesNotMatchDuringLogin() {
        LoginDTO loginDTO = new LoginDTO("email@test.com", "password");
        Client user = TestClientFactory.standard();
        
        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.password(), user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.login(loginDTO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Os dados informados são inválidos");
    }

    @Test
    void shouldThrowExceptionWhenUserIsBannedDuringLogin() {
        LoginDTO loginDTO = new LoginDTO("email@test.com", "password");
        Client user = TestClientFactory.builder()
                .status(UserStatus.BANNED)
                .build();

        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.password(), user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.login(loginDTO))
                .isInstanceOf(LoginException.class)
                .hasMessageContaining("Você foi banido deste estabelecimento");
    }

    @Test
    void shouldThrowExceptionWhenUserBelongsToDifferentTenantDuringLogin() {
        LoginDTO loginDTO = new LoginDTO("email@test.com", "password");
        Client user = TestClientFactory.builder()
                .tenantId("other-tenant")
                .userRole(UserRole.CLIENT)
                .build();

        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.password(), user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.login(loginDTO))
                .isInstanceOf(LoginException.class)
                .hasMessageContaining("Acesso negado para este estabelecimento.");
    }

    @Test
    void shouldAllowLoginForSuperAdminEvenWithDifferentTenant() {
        LoginDTO loginDTO = new LoginDTO("admin@test.com", "password");
        Client user = TestClientFactory.builder()
                .tenantId("other-tenant")
                .userRole(UserRole.SUPER_ADMIN)
                .build();

        when(userRepository.findByEmailIgnoreCase(loginDTO.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginDTO.password(), user.getPassword())).thenReturn(true);
        when(tokenService.generateAuthToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(RefreshToken.builder().token("refresh-token").build());

        AuthResultDTO result = authenticationService.login(loginDTO);

        assertThat(result.jwtToken()).isEqualTo("jwt-token");
    }

    @Test
    void shouldRevokeTokenOnLogout() {
        authenticationService.logout("refresh-token", 1L);
        verify(refreshTokenService).revokeUserToken("refresh-token", 1L);
    }

    @Test
    void shouldRefreshTokenSuccessfullyWhenTokenIsValid() {
        String oldTokenStr = "old-refresh-token";
        Client user = TestClientFactory.standard();
        RefreshToken oldToken = RefreshToken.builder()
                .token(oldTokenStr)
                .user(user)
                .isRevoked(false)
                .build();

        when(refreshTokenService.findByToken(oldTokenStr)).thenReturn(Optional.of(oldToken));
        when(refreshTokenService.verifyExpiration(oldToken)).thenReturn(oldToken);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(RefreshToken.builder().token("new-refresh-token").build());
        when(tokenService.generateAuthToken(user)).thenReturn("new-jwt-token");

        TokenRefreshResponseDTO result = authenticationService.refreshToken(oldTokenStr);

        assertThat(result.jwtToken()).isEqualTo("new-jwt-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(oldToken.isRevoked()).isTrue();
    }

    @Test
    void shouldRevokeAllTokensAndThrowExceptionIfTokenIsAlreadyRevoked() {
        String tokenStr = "revoked-token";
        Client user = TestClientFactory.builder().id(1L).build();
        RefreshToken token = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .isRevoked(true)
                .build();

        when(refreshTokenService.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authenticationService.refreshToken(tokenStr))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Este token já foi utilizado.");

        verify(refreshTokenService).revokeAllForUser(1L);
    }

    @Test
    void shouldThrowExceptionWhenTokenIsNotFoundDuringRefresh() {
        when(refreshTokenService.findByToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.refreshToken("non-existent"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("Token não encontrado");
    }
}
