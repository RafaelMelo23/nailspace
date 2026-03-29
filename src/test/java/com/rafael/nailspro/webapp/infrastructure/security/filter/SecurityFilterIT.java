package com.rafael.nailspro.webapp.infrastructure.security.filter;

import com.rafael.nailspro.webapp.infrastructure.security.token.TokenService;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import com.rafael.nailspro.webapp.support.factory.TestProfessionalFactory;
import com.rafael.nailspro.webapp.support.factory.TestSalonProfileFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityFilterIT extends BaseIntegrationTest {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private MockMvc mvc;

    @Test
    void doFilterInternal_validatesAccess_whenUserIsAuthenticated() throws Exception {
        var client = clientRepository.save(TestClientFactory.standardForIt());

        String token = tokenService.generateAuthToken(client);

        mvc.perform(get("/api/v1/professional/simplified")
                        .header(HttpHeaders.HOST, "tenant-test.localhost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void doFilterInternal_rejectsAccess_whenTokenIsMissing() throws Exception {
        mvc.perform(get("/api/v1/user")
                        .header(HttpHeaders.HOST, "tenant-test.localhost"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotFilter_allowsAccessToWebhookWithoutToken() throws Exception {

        mvc.perform(post("/api/v1/webhook")
                        .header("apiKey", "test")
                        .content("{}")
                        .contentType("application/json"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 401 || status == 403) {
                        throw new AssertionError("Expected access to be allowed, but was " + status);
                    }
                });
    }

    @Test
    void doFilterInternal_ignoresToken_whenPurposeIsWrong() throws Exception {
        var client = clientRepository.save(TestClientFactory.standardForIt());

        String token = tokenService.generateResetPasswordToken(client.getId());

        mvc.perform(get("/api/v1/user")
                        .header(HttpHeaders.HOST, "tenant-test.localhost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doFilterInternal_rejectsAccess_whenUserRoleIsInsufficient() throws Exception {
        var client = clientRepository.save(TestClientFactory.standardForIt());

        String token = tokenService.generateAuthToken(client);

        mvc.perform(get("/api/v1/admin/appointments/" + client.getId())
                        .header(HttpHeaders.HOST, "tenant-test.localhost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void doFilterInternal_correctlyMapsPrincipal() throws Exception {
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional));

        String token = tokenService.generateAuthToken(professional);

        mvc.perform(get("/api/v1/professional/schedule/block")
                        .param("dateAndTime", LocalDateTime.now().toString())
                        .header(HttpHeaders.HOST, "tenant-test.localhost")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}