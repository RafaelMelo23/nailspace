package com.rafael.nailspro.webapp.domain.repository;

import com.rafael.nailspro.webapp.domain.enums.user.UserStatus;
import com.rafael.nailspro.webapp.domain.model.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByPhoneNumber(String phoneNumber);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Client c
    SET c.status = :status
    WHERE c.id = :id
""")
    void updateClientStatus(@Param("id") Long clientId, @Param("status") UserStatus status);

    Page<Client> findByFullNameContainingIgnoreCase(String name, Pageable pageable);

    @Modifying
    @Query("UPDATE Client cl SET cl.canceledAppointments = cl.canceledAppointments + 1 WHERE cl.id = :clientId")
    void incrementCanceledAppointments(@Param("clientId") Long clientId);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Client> findByEmailIgnoreCase(String email);
}
