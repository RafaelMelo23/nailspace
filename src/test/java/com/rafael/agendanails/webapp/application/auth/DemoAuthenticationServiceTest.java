package com.rafael.agendanails.webapp.application.auth;

import com.rafael.agendanails.webapp.application.salon.business.SalonServiceService;
import com.rafael.agendanails.webapp.domain.enums.demo.DemoUserType;
import com.rafael.agendanails.webapp.domain.model.Client;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.RefreshToken;
import com.rafael.agendanails.webapp.domain.model.User;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.domain.repository.UserRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.auth.AuthResultDTO;
import com.rafael.agendanails.webapp.infrastructure.security.token.JwtTokenService;
import com.rafael.agendanails.webapp.infrastructure.security.token.RefreshTokenService;
import com.rafael.agendanails.webapp.support.factory.TestSalonServiceFactory;
import com.rafael.agendanails.webapp.support.factory.TestUserPrincipalFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoAuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EntityManager entityManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SalonServiceService salonService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private DemoAuthenticationService demoAuthenticationService;

    private static final String DEMO_TENANT = "demo-salon-2026";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(demoAuthenticationService, "demoTenant", DEMO_TENANT);
    }

    @Test
    void shouldCreateAndLoginDemoClient() {
        when(passwordEncoder.encode(anyString()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(1L);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtTokenService.generateAuthToken(any(UserPrincipal.class))).thenReturn("jwt");
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(refreshToken.getToken()).thenReturn("refresh");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);

        AuthResultDTO result = demoAuthenticationService.createAndLoginDemoUser(DemoUserType.CLIENT);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isInstanceOf(Client.class);
        assertThat(savedUser.getTenantId()).isEqualTo(DEMO_TENANT);
        assertThat(savedUser.getEmail()).contains("demo.client");
        
        verify(entityManager).flush();
        verify(authenticationManager).authenticate(argThat(auth -> 
            auth.getPrincipal().equals(savedUser.getEmail()) && auth.getCredentials().equals("123456")
        ));
        
        assertThat(result.jwtToken()).isEqualTo("jwt");
    }

    @Test
    void shouldCreateAndLoginDemoProfessional() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(salonService.findAll()).thenReturn(Set.of(TestSalonServiceFactory.standard()));

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(1L);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(jwtTokenService.generateAuthToken(any(UserPrincipal.class))).thenReturn("jwt");
        RefreshToken refreshToken = mock(RefreshToken.class);
        when(refreshToken.getToken()).thenReturn("refresh");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn(refreshToken);

        AuthResultDTO result = demoAuthenticationService.createAndLoginDemoUser(DemoUserType.PROFESSIONAL);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertThat(savedUser).isInstanceOf(Professional.class);
        assertThat(savedUser.getTenantId()).isEqualTo(DEMO_TENANT);
        assertThat(savedUser.getEmail()).contains("demo.profissional");
        
        Professional professional = (Professional) savedUser;
        assertThat(professional.getWorkSchedules()).isNotEmpty();
        
        verify(entityManager).flush();
        verify(authenticationManager).authenticate(argThat(auth -> 
            auth.getPrincipal().equals(savedUser.getEmail()) && auth.getCredentials().equals("123456")
        ));
        
        assertThat(result.jwtToken()).isEqualTo("jwt");
    }
}

