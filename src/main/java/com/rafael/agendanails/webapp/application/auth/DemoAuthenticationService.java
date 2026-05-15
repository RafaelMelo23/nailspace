package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.application.salon.business.SalonServiceService;
import com.rafael.agendanails.webapp.domain.enums.demo.DemoUserType;
import com.rafael.agendanails.webapp.domain.model.*;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.AuthResultDTO;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.infrastructure.security.token.RefreshTokenService;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class DemoAuthenticationService {

    @Value("${demo.tenant}")
    private String demoTenant;

    private final SalonServiceService salonServiceService;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthResultDTO createAndLoginDemoUser(DemoUserType demoUserType) {
        TenantContext.setTenant(demoTenant);

        User demoUser = switch (demoUserType) {
            case CLIENT -> userRepository.save(createDemoClient());
            case PROFESSIONAL -> {
                var services = salonServiceService.findAll();
                Professional demoProfessional = createDemoProfessional(services);

                userRepository.save(demoProfessional);
                salonServiceService.saveAll(services);
                yield demoProfessional;
            }
        };

        entityManager.flush();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(demoUser.getEmail(), "123456")
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        String jwt = jwtTokenService.generateAuthToken(principal);
        String refresh = refreshTokenService.createRefreshToken(principal.getId()).getToken();

        return AuthResultDTO.builder()
                .jwtToken(jwt)
                .refreshToken(refresh).build();
    }

    public Professional createDemoProfessional(Set<SalonService> services) {
        int number = generateDemoNumber();

        String name = "Profissional Demo " + number;
        String email = "demo.profissional" + number + "@nailspace.com";
        String hashedPassword = passwordEncoder.encode("123456");

        Professional adminProfessional = Professional.createAdminProfessional(
                name,
                email);

        adminProfessional.changePassword(hashedPassword);
        adminProfessional.assignTenant(demoTenant);
        adminProfessional.markFirstLoginDone();
        adminProfessional.assignWorkSchedules(WorkSchedule.createDefaultWeek(adminProfessional));
        if (services != null) {
            adminProfessional.assignSalonServices(services);
            services.forEach(service -> service.getProfessionals().add(adminProfessional));
        }
        return adminProfessional;
    }

    public Client createDemoClient() {
        int number = generateDemoNumber();

        String name = "Client Demo " + number;
        String email = "demo.client" + number + "@nailspace.com";
        String hashedPassword = passwordEncoder.encode("123456");

        Client demoClient = Client.createDefault(name, email, hashedPassword);
        demoClient.assignTenant(demoTenant);
        return demoClient;
    }

    private static int generateDemoNumber() {
        return 1000 + ThreadLocalRandom.current().nextInt(9000);
    }
}
