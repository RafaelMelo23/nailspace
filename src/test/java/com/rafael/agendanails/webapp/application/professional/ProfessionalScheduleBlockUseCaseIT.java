package com.rafael.agendanails.webapp.application.professional;

import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.ScheduleBlock;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.schedule.block.ScheduleBlockOutDTO;
import com.rafael.agendanails.webapp.shared.tenant.TenantContext;
import com.rafael.agendanails.webapp.support.BaseIntegrationTest;
import com.rafael.agendanails.webapp.support.factory.TestProfessionalFactory;
import com.rafael.agendanails.webapp.support.factory.TestSalonProfileFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalScheduleBlockUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private ProfessionalScheduleBlockUseCase useCase;

    @Test
    void shouldCreateBlock() {
        Professional pro = professionalRepository.save(TestProfessionalFactory.builder().build());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(pro));
        
        ZonedDateTime now = ZonedDateTime.now();
        ScheduleBlockDTO dto = new ScheduleBlockDTO(
                null,
                now,
                now.plusHours(2),
                false,
                "Lunch"
        );

        useCase.createBlock(dto, pro.getId());

        List<ScheduleBlock> blocks = scheduleBlockRepository.findByProfessional_Id(pro.getId());
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getReason()).isEqualTo("Lunch");
    }

    @Test
    void shouldDeleteBlock() {
        Professional pro = professionalRepository.save(TestProfessionalFactory.builder().build());
        ZonedDateTime now = ZonedDateTime.now();
        ScheduleBlock block = scheduleBlockRepository.save(ScheduleBlock.builder()
                .professional(pro)
                .dateStartTime(now.toInstant())
                .dateEndTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId("tenant-test")
                .build());

        useCase.deleteBlock(block.getId(), pro.getId());

        assertThat(scheduleBlockRepository.findById(block.getId())).isEmpty();
    }

    @Test
    void shouldGetBlocksWithTenantIsolation() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";

        TenantContext.setTenant(tenantA);
        Professional proA = professionalRepository.save(TestProfessionalFactory.builder().tenantId(tenantA).build());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(proA, tenantA));
        ZonedDateTime now = ZonedDateTime.now();
        scheduleBlockRepository.save(ScheduleBlock.builder()
                .professional(proA)
                .dateStartTime(now.toInstant())
                .dateEndTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId(tenantA)
                .build());

        TenantContext.setTenant(tenantB);
        Professional proB = professionalRepository.save(TestProfessionalFactory.builder().tenantId(tenantB).build());
        salonProfileRepository.save(TestSalonProfileFactory.standardForIT(proB, tenantB));
        scheduleBlockRepository.save(ScheduleBlock.builder()
                .professional(proB)
                .dateStartTime(now.toInstant())
                .dateEndTime(now.plusHours(1).toInstant())
                .isWholeDayBlocked(false)
                .reason("Test")
                .tenantId(tenantB)
                .build());

        TenantContext.setTenant(tenantA);
        UserPrincipal principalA = UserPrincipal.builder()
                .userId(proA.getId())
                .tenantId(tenantA)
                .email(proA.getEmail())
                .userRole(proA.getEffectiveRoles())
                .build();
        List<ScheduleBlockOutDTO> blocksA = useCase.getBlocks(principalA, null);

        assertThat(blocksA).hasSize(1);
    }
}
