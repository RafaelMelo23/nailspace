package com.rafael.nailspro.webapp.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(name = "salon_daily_revenue")
@Filter(name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
public class SalonDailyRevenue extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String tenantId;
    private LocalDate date;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalRevenue;

    private Long appointmentsCount;

    public SalonDailyRevenue(String tenantId, LocalDate date) {
        this.tenantId = tenantId;
        this.date = date;
        this.totalRevenue = BigDecimal.ZERO;
        this.appointmentsCount = 0L;
    }
}
