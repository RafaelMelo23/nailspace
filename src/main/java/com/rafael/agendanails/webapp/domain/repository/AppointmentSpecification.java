package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class AppointmentSpecification {

    public static Specification<Appointment> withProfessionalId(Long professionalId) {
        return (root, query, cb) ->
                professionalId == null ? null : cb.equal(root.get("professional").get("id"), professionalId);
    }

    public static Specification<Appointment> withStatus(AppointmentStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("appointmentStatus"), status);
    }

    public static Specification<Appointment> withDate(LocalDate date, ZoneId zoneId) {
        if (date == null) return null;
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant startOfNextDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("startDate"), startOfDay),
                cb.lessThan(root.get("startDate"), startOfNextDay)
        );
    }

    public static Specification<Appointment> withDateRange(Instant start, Instant end) {
        if (start == null || end == null) return null;
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("startDate"), start),
                cb.lessThan(root.get("startDate"), end)
        );
    }

    public static Specification<Appointment> fetchSummaryRelationships() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("client", JoinType.LEFT);
                root.fetch("professional", JoinType.LEFT);
                root.fetch("mainSalonService", JoinType.LEFT);
            }
            return null;
        };
    }

    public static Specification<Appointment> fetchRelationships() {
        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("client", JoinType.LEFT);
                root.fetch("professional", JoinType.LEFT);
                root.fetch("mainSalonService", JoinType.LEFT);
                root.fetch("addOns", JoinType.LEFT).fetch("service", JoinType.LEFT);
            }
            return null;
        };
    }
}
