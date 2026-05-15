package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.SalonProfile;
import com.rafael.agendanails.webapp.domain.model.ScheduleBlock;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonProfileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalScheduleBlockUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private ProfessionalScheduleBlockUseCase useCase;

    private Professional professional;
    private SalonProfile salonProfile;

    @BeforeEach
    void setUp() {
        salon(TestProfessionalFactory.builder().build(), "tenant-test");
    }

    private void salon(Professional pro, String tenant) {
        TenantContext.setTenant(tenant);
        this.professional = professionalRepository.save(pro);
        this.salonProfile = salonProfileRepository.save(TestSalonProfileFactory.standardForIT(professional, tenant));
    }

    @Test
    void shouldCreateBlock() {
        ZonedDateTime now = ZonedDateTime.now();
        ScheduleBlockDTO dto = new ScheduleBlockDTO(
                null,
                now,
                now.plusHours(2),
                false,
                "Lunch"
        );

        useCase.createBlock(dto, professional.getId());

        List<ScheduleBlock> blocks = scheduleBlockRepository.findByProfessional_Id(professional.getId());
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getReason()).isEqualTo("Lunch");
    }

    @Test
    void shouldDeleteBlock() {
        ZonedDateTime now = ZonedDateTime.now();
        ScheduleBlock block = scheduleBlockRepository.save(ScheduleBlock.testBuilder()
                .professional(professional)
                .startTime(now.toInstant())
                .endTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId("tenant-test")
                .build());

        useCase.deleteBlock(block.getId(), professional.getId());

        assertThat(scheduleBlockRepository.findById(block.getId())).isEmpty();
    }

    @Test
    void shouldGetBlocksWithTenantIsolation() {
        ZonedDateTime now = ZonedDateTime.now();
        scheduleBlockRepository.save(ScheduleBlock.testBuilder()
                .professional(professional)
                .startTime(now.toInstant())
                .endTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId("tenant-test")
                .build());

        String tenantB = "tenant-b";
        TenantContext.setTenant(tenantB);
        Professional proB = professionalRepository.save(TestProfessionalFactory.builder().tenantId(tenantB).build());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(proB, tenantB));

        scheduleBlockRepository.save(ScheduleBlock.testBuilder()
                .professional(proB)
                .startTime(now.toInstant())
                .endTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId(tenantB)
                .build());

        TenantContext.setTenant("tenant-test");
        UserPrincipal principal = UserPrincipal.builder()
                .id(professional.getId())
                .tenantId("tenant-test")
                .email(professional.getEmail())
                .userRole(professional.getEffectiveRoles())
                .build();

        List<ScheduleBlockOutDTO> blocks = useCase.getBlocks(principal, null);

        assertThat(blocks).hasSize(1);
    }
}
