package com.rafael.nailspro.webapp.support;

import com.rafael.nailspro.webapp.SchedulingNailsProApplication;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.repository.*;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(classes = SchedulingNailsProApplication.class)
@ActiveProfiles("it")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseIntegrationTest {

    @Autowired
    protected AppointmentRepository appointmentRepository;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected ProfessionalRepository professionalRepository;
    @Autowired
    protected SalonServiceRepository salonServiceRepository;
    @Autowired
    protected SalonProfileRepository salonProfileRepository;
    @Autowired
    protected WorkScheduleRepository workScheduleRepository;
    @Autowired
    protected ScheduleBlockRepository scheduleBlockRepository;
    @Autowired
    protected WhatsappMessageRepository whatsappMessageRepository;
    @Autowired
    protected RetentionForecastRepository retentionForecastRepository;
    @Autowired
    protected ClientAuditMetricsRepository clientAuditMetricsRepository;

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("nailspro_it")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @org.junit.jupiter.api.BeforeEach
    void globalSetUp() {
        TenantContext.setTenant("tenant-test");
    }

    @AfterEach
    void globalTearDown() {
        TenantContext.setIgnoreFilter(true);
        try {
            whatsappMessageRepository.deleteAllInBatch();
            retentionForecastRepository.deleteAllInBatch();
            appointmentRepository.deleteAllInBatch();
            scheduleBlockRepository.deleteAllInBatch();
            workScheduleRepository.deleteAllInBatch();
            salonServiceRepository.deleteAllInBatch();
            salonProfileRepository.deleteAllInBatch();
            clientAuditMetricsRepository.deleteAllInBatch();
            clientRepository.deleteAllInBatch();
            professionalRepository.deleteAllInBatch();
        } finally {
            TenantContext.clear();
        }
    }
}
