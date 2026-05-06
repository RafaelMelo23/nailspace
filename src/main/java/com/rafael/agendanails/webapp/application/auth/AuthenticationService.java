package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.repository.ClientRepository;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.RegisterDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.UserAlreadyExistsException;
import com.rafael.agendanails.webapp.infrastructure.security.token.RefreshTokenService;
import com.rafael.agendanails.webapp.shared.tenant.IgnoreTenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.rafael.agendanails.webapp.infrastructure.helper.PhoneNumberHelper.formatPhoneNumber;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
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
    public void logout(String refreshToken, Long userId) {
        refreshTokenService.revokeUserToken(refreshToken, userId);
    }
}