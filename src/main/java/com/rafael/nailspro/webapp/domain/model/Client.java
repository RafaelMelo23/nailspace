package com.rafael.nailspro.webapp.domain.model;

import com.rafael.nailspro.webapp.domain.enums.user.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

import java.util.List;

@Entity
@SuperBuilder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@PrimaryKeyJoinColumn(name = "user_id")
@Table(name = "clients",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_phone_per_tenant",
                        columnNames = {"tenant_id", "phone_number"})
        })
public class Client extends User {

    @Column(name = "missed_appointments", nullable = false)
    private Integer missedAppointments = 0;

    @Column(name = "cancelled_appointments", nullable = false)
    private Integer canceledAppointments = 0;

    @OneToMany(mappedBy = "client")
    private List<Appointment> clientAppointments;

    @Column(name = "phone_number", nullable = false, length = 13)
    private String phoneNumber;

    @OneToOne(mappedBy = "client", orphanRemoval = true)
    private ClientAuditMetrics clientAuditMetrics;

    @Override
    public void prePersist() {
        super.prePersist();

        setUserRole(UserRole.CLIENT);

        if (this.missedAppointments == null) this.missedAppointments = 0;
        if (this.canceledAppointments == null) this.setCanceledAppointments(0);
    }

    public static String extractFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "cliente";

        return fullName.split("\\s+")[0];
    }
}