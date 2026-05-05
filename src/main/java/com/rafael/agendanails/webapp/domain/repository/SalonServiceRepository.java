package com.rafael.agendanails.webapp.domain.repository;

import com.rafael.agendanails.webapp.domain.model.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SalonServiceRepository extends JpaRepository<SalonService, Long> {

    Set<SalonService> findByIdIn(Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE SalonService ss SET ss.active = :active where ss.id = :id")
    void changeSalonServiceVisibility(@Param("id") Long id,
                                      @Param("active") Boolean active);

    @Query("SELECT DISTINCT ss FROM SalonService ss LEFT JOIN FETCH ss.professionals WHERE ss.active = TRUE")
    Set<SalonService> findAllServices();

    @Query("SELECT DISTINCT ss FROM SalonService ss LEFT JOIN FETCH ss.professionals")
    List<SalonService> findAllWithInactive();
}
