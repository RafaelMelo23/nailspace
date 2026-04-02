package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.model.User;
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
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.rafael.agendanails.webapp.domain.enums.user.UserStatus.BANNED;
import static com.rafael.agendanails.webapp.infrastructure.helper.PhoneNumberHelper.formatPhoneNumber;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void register(RegisterDTO registerDTO) {
        checkIfUserAlreadyExists(registerDTO);

        String encryptedPassword = passwordEncoder.encode(registerDTO.rawPassword());
        String normalizedPhoneNumber = formatPhoneNumber(registerDTO.phoneNumber());

        clientRepository.save(Client.builder()
                .fullName(registerDTO.fullName())
                .email(registerDTO.email())
                .password(encryptedPassword)
                .status(UserStatus.ACTIVE)
                .phoneNumber(normalizedPhoneNumber)
                .build()
        );
    }

    private void checkIfUserAlreadyExists(RegisterDTO registerDTO) {
        userRepository.findByEmailIgnoreCase(registerDTO.email())
                .ifPresent(user -> {
                    throw new UserAlreadyExistsException("O E-mail informado já está sendo utilizado");
                });
        clientRepository.findByPhoneNumber(formatPhoneNumber(registerDTO.phoneNumber()))
                .ifPresent(user -> {
                    throw new UserAlreadyExistsException("O telefone informado já está sendo utilizado");
                });
    }

    @Transactional
    @IgnoreTenantFilter
    public AuthResultDTO login(LoginDTO loginDTO) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(loginDTO.email());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();

            checkUserPassword(loginDTO, user);
            checkUserStatus(user);
            checkUsersTenant(user);
        } else {
            passwordEncoder.matches(loginDTO.password(), "$2a$12$p1DeDmHwMBRxNGAJ7II9JefEvHnrPDxCw72YF0nh1Modhwv67y1hK");
            throw new BusinessException("Os dados informados são inválidos");
        }

        String jwt = tokenService.generateAuthToken(user);
        String refresh = refreshTokenService.createRefreshToken(user).getToken();

        return AuthResultDTO.builder()
                .jwtToken(jwt)
                .refreshToken(refresh).build();
    }

    private static void checkUsersTenant(User user) {
        if (user.getUserRole().equals(UserRole.SUPER_ADMIN)) return;

        if (!user.getTenantId().equals(TenantContext.getTenant())) {
            throw new LoginException("Acesso negado para este estabelecimento.");
        }
    }

    private void checkUserPassword(LoginDTO loginDTO, User user) {
        if (!passwordEncoder.matches(loginDTO.password(), user.getPassword())) {
            throw new BusinessException("Os dados informados são inválidos");
        }
    }

    private static void checkUserStatus(User user) {
        if (user.getStatus().equals(BANNED)) {
            throw new LoginException("Você foi banido deste estabelecimento");
        }
    }

    @Transactional
    @IgnoreTenantFilter
    public void logout(String refreshToken, Long userId) {
        refreshTokenService.revokeUserToken(refreshToken, userId);
    }

    @Transactional(noRollbackFor = TokenRefreshException.class)
    @IgnoreTenantFilter
    public TokenRefreshResponseDTO refreshToken(String refreshToken) {
        return refreshTokenService.findByToken(refreshToken)
                .map(token -> {
                    if (token.isRevoked()) {
                        refreshTokenService.revokeAllForUser(token.getUser().getId());
                        throw new TokenRefreshException("Este token já foi utilizado.");
                    }
                    return token;
                })
                .map(refreshTokenService::verifyExpiration)
                .map(token -> {
                    User user = token.getUser();
                    token.setRevoked(true);

                    RefreshToken newRefresh = refreshTokenService.createRefreshToken(user);
                    String newJwt = tokenService.generateAuthToken(user);

                    return new TokenRefreshResponseDTO(newJwt, newRefresh.getToken());
                })
                .orElseThrow(() -> new TokenRefreshException("Token não encontrado"));
    }
}