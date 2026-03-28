package com.rafael.nailspro.webapp.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "client_audit_metrics")
public class ClientAuditMetrics extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "total_spent", precision = 19, scale = 2)
    private BigDecimal totalSpent;

    @Column(name = "completed_appointments_count")
    private Long completedAppointmentsCount;

    @Column(name = "canceled_appointments_count")
    private Long canceledAppointmentsCount;

    @Column(name = "missed_appointments_count")
    private Long missedAppointmentsCount;

    @Column(name = "last_visit_date")
    private ZonedDateTime lastVisitDate;

    public ClientAuditMetrics(String tenantId, Client client) {

        setTenantId(tenantId);
        this.client = client;
        this.totalSpent = BigDecimal.ZERO;
        this.completedAppointmentsCount = 0L;
        this.canceledAppointmentsCount = 0L;
        this.missedAppointmentsCount = 0L;
        this.lastVisitDate = ZonedDateTime.now();
    }
}