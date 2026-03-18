package com.rafael.nailspro.webapp.support.factory;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.ScheduleBlock;
import com.rafael.nailspro.webapp.domain.model.WorkSchedule;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class TestProfessionalFactory {

    public static Professional standard() {
        return baseBuilder().build();
    }

    public static Professional inactive() {
        return baseBuilder()
                .isActive(false)
                .build();
    }

    public static Professional firstLogin() {
        return baseBuilder()
                .isFirstLogin(true)
                .build();
    }

    public static Professional withSchedules(Set<WorkSchedule> schedules) {
        Professional professional = standard();
        professional.setWorkSchedules(schedules);

        schedules.forEach(schedule -> schedule.setProfessional(professional));

        return professional;
    }

    public static Professional withBlocks(Set<ScheduleBlock> blocks) {
        Professional professional = standard();
        professional.setScheduleBlocks(blocks);

        blocks.forEach(block -> block.setProfessional(professional));

        return professional;
    }

    private static Professional.ProfessionalBuilder<?, ?> baseBuilder() {
        return Professional.builder()
                .id(1L)

                // User fields
                .fullName("Test Professional")
                .email("professional@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.PROFESSIONAL)

                // Professional fields
                .externalId(UUID.randomUUID())
                .professionalPicture("test-picture.png")
                .isActive(true)
                .isFirstLogin(false)
                .workSchedules(new HashSet<>())
                .scheduleBlocks(new LinkedHashSet<>());
    }
}