package com.rafael.nailspro.webapp.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@SuperBuilder
@Getter @Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "email_usage_quota")
public class EmailUsageQuota {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    private Integer dailyCount;

    public void increment() {
        this.dailyCount++;
    }
}