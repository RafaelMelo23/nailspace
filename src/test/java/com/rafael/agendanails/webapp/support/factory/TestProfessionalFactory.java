package com.rafael.agendanails.webapp.support.factory;

import com.rafael.agendanails.webapp.domain.enums.user.UserRole;
import com.rafael.agendanails.webapp.domain.enums.user.UserStatus;
import com.rafael.agendanails.webapp.domain.model.Professional;
import com.rafael.agendanails.webapp.domain.model.ScheduleBlock;
import com.rafael.agendanails.webapp.domain.model.WorkSchedule;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TestProfessionalFactory {

    private TestProfessionalFactory() {}

    public static Professional.ProfessionalBuilder<?, ?> builder() {
        String unique = UUID.randomUUID().toString();
        return Professional.builder()
                .fullName("Test Professional " + unique)
                .email("professional+" + unique + "@test.com")
                .password("encoded-password")
                .status(UserStatus.ACTIVE)
                .userRole(UserRole.PROFESSIONAL)
                .externalId(UUID.randomUUID())
                .professionalPicture("test-picture.png")
                .isActive(true)
                .isFirstLogin(false)
                .workSchedules(new HashSet<>())
                .tenantId("tenant-test")
                .scheduleBlocks(new LinkedHashSet<>());
    }

    public static Professional standard() {
        return builder().id(nextId()).build();
    }

    public static Professional standardEnglish() {
        return builder()
                .id(nextId())
                .fullName("John Professional")
                .tenantId("tenant-test")
                .build();
    }

    public static Professional standardForIt() {
        return builder().build();
    }

    public static Professional standardForIt(String tenantId) {
        return builder().tenantId(tenantId).build();
    }

    public static Professional inactive() {
        return builder().id(nextId()).isActive(false).build();
    }

    public static Professional firstLogin() {
        return builder().id(nextId()).isFirstLogin(true).build();
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

    private static long nextId() {
        return ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
    }
}