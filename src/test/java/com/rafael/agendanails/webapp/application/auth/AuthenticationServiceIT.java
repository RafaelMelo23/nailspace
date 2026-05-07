package com.rafael.agendanails.webapp.application.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.domain.repository.ProfessionalRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.AuthResultDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.LoginDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.RegisterDTO;
import com.rafael.agendanails.webapp.infrastructure.exception.BusinessException;
import com.rafael.agendanails.webapp.infrastructure.exception.LoginException;
import com.rafael.agendanails.webapp.infrastructure.exception.TokenRefreshException;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestClientFactory;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonProfileFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthenticationServiceIT extends BaseIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @Test
    void shouldRegisterNewClientWhenDataIsValid() {
        RegisterDTO dto = new RegisterDTO("IT Test User", "it-test@example.com", "securePass123", "11988887777");

        authenticationService.register(dto);

        Optional<User> persistedUser = userRepository.findByEmailIgnoreCase("it-test@example.com");

        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getFullName()).isEqualTo("IT Test User");
        assertThat(passwordEncoder.matches("securePass123", persistedUser.get().getPassword())).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenLoginPasswordIsIncorrect() throws Exception {
        Client client = TestClientFactory.standardForIt();
        client.setPassword(passwordEncoder.encode("correct-password"));
        userRepository.save(client);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new LoginDTO(client.getEmail(), "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldGenerateAndPersistTokensWhenLoginIsSuccessful() throws Exception {
        Client client = TestClientFactory.standardForIt();
        client.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(client);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(new LoginDTO(client.getEmail(), "password123"))))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    assertThat(content).contains("jwtToken");
                    
                    var cookie = result.getResponse().getCookie("refresh_token");
                    assertThat(cookie).isNotNull();
                    String refreshToken = cookie.getValue();
                    assertThat(refreshToken).isNotBlank();

                    TenantContext.setTenant("tenant-test");
                    Optional<RefreshToken> refresh = refreshTokenRepository.findByToken(refreshToken);
                    assertThat(refresh).isPresent();
                    assertThat(refresh.get().getUser().getId()).isEqualTo(client.getId());
                });
    }

    @Test
    void shouldThrowExceptionWhenUserBelongsToDifferentTenant() throws Exception {
        Client client = TestClientFactory.standardForIt("other-tenant");
        client.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(client);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "my-tenant")
                        .content(mapper.writeValueAsBytes(new LoginDTO(client.getEmail(), "password123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowLoginWhenUserIsSuperAdminRegardlessOfTenant() throws Exception {
        var admin = TestProfessionalFactory.builder()
                .userRole(UserRole.SUPER_ADMIN)
                .tenantId("admin-tenant")
                .email("super@admin.com")
                .password(passwordEncoder.encode("admin123"))
                .build();
        userRepository.save(admin);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "some-salon-tenant")
                        .content(mapper.writeValueAsBytes(new LoginDTO("super@admin.com", "admin123"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRevokeAllTokensWhenRevokedTokenIsUsed() throws Exception {
        Client client = TestClientFactory.standardForIt();
        userRepository.save(client);

        RefreshToken t1 = RefreshToken.builder().token("t1").user(client).expiryDate(Instant.now().plusSeconds(1000)).tenantId("tenant-test").isRevoked(false).build();
        RefreshToken t2 = RefreshToken.builder().token("t2").user(client).expiryDate(Instant.now().plusSeconds(1000)).tenantId("tenant-test").isRevoked(false).build();
        RefreshToken t3 = RefreshToken.builder().token("t3").user(client).expiryDate(Instant.now().plusSeconds(1000)).tenantId("tenant-test").isRevoked(true).build();
        refreshTokenRepository.saveAll(List.of(t1, t2, t3));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "t3")))
                .andExpect(status().isUnauthorized());

        entityManager.flush();
        TenantContext.setTenant("tenant-test");
        List<RefreshToken> all = refreshTokenRepository.findAll();
        assertThat(all).extracting(RefreshToken::isRevoked).containsOnly(true);
    }

    @Test
    void shouldReturn401WhenUserIsBanned() throws Exception {
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, "tenant-test"));
        Client client = TestClientFactory.standardForIt();
        client.setPassword(passwordEncoder.encode("whatever"));
        client.setStatus(UserStatus.BANNED);
        userRepository.save(client);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "tenant-test")
                        .content(mapper.writeValueAsBytes(new LoginDTO(client.getEmail(), "whatever"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRotateTokensWhenRefreshIsSuccessful() throws Exception {
        Client client = TestClientFactory.standardForIt();
        userRepository.save(client);

        RefreshToken oldToken = RefreshToken.builder()
                .token("old-token")
                .user(client)
                .expiryDate(Instant.now().plusSeconds(1000))
                .isRevoked(false)
                .tenantId("tenant-test")
                .build();
        refreshTokenRepository.save(oldToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", "old-token")))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String newJwt = result.getResponse().getContentAsString();
                    assertThat(newJwt).isNotEmpty();

                    var cookie = result.getResponse().getCookie("refresh_token");
                    assertThat(cookie).isNotNull();
                    String newRefreshToken = cookie.getValue();
                    assertThat(newRefreshToken).isNotEqualTo("old-token");

                    TenantContext.setTenant("tenant-test");

                    RefreshToken updatedOldToken = refreshTokenRepository.findByToken("old-token").orElseThrow();
                    assertThat(updatedOldToken.isRevoked()).isTrue();

                    Optional<RefreshToken> persistedNewToken = refreshTokenRepository.findByToken(newRefreshToken);
                    assertThat(persistedNewToken).isPresent();
                });
    }
}
