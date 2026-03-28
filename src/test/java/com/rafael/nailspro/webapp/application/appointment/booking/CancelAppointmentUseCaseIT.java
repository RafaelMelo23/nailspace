package com.rafael.nailspro.webapp.application.appointment.booking;

import com.rafael.nailspro.webapp.domain.enums.appointment.AppointmentStatus;
import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.Client;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonService;
import com.rafael.nailspro.webapp.domain.repository.*;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.shared.tenant.TenantContext;
import com.rafael.nailspro.webapp.support.BaseIntegrationTest;
import com.rafael.nailspro.webapp.support.factory.TestAppointmentFactory;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import com.rafael.nailspro.webapp.support.factory.TestProfessionalFactory;
import com.rafael.nailspro.webapp.support.factory.TestSalonServiceFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("it")
class CancelAppointmentUseCaseIT extends BaseIntegrationTest {

    @Autowired
    private CancelAppointmentUseCase cancelAppointmentUseCase;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ProfessionalRepository professionalRepository;
    @Autowired
    private SalonServiceRepository salonServiceRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private SalonProfileRepository salonProfileRepository;
    @Autowired
    private WorkScheduleRepository workScheduleRepository;

    private @NotNull PreparationData getPreparationData() {
        var client = clientRepository.save(TestClientFactory.standardForIt());
        var professional = professionalRepository.save(TestProfessionalFactory.standardForIt());
        var service = salonServiceRepository.save(TestSalonServiceFactory.standardForIt());

        return new PreparationData(client, professional, service);
    }

    private record PreparationData(Client client,
                                   Professional professional,
                                   SalonService service) {
    }

    @Test
    void cancelAppointment_shouldHandleCancellationAndNotStrikeClient() {
        PreparationData prep = getPreparationData();

        Instant start = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        end,
                        prep.client(),
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.CONFIRMED
                )
        );

        cancelAppointmentUseCase.cancelAppointment(appointment.getId(), prep.client().getId());
        Client client = clientRepository.findById(prep.client().getId())
                .orElse(null);

        appointment = appointmentRepository.findById(appointment.getId())
                .orElse(null);

        assertThat(appointment.getAppointmentStatus())
                .isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(client.getCanceledAppointments())
                .isEqualTo(0);
    }

    @Test
    void cancelAppointment_shouldHandleCancellationAndStrikeClient() {
        PreparationData prep = getPreparationData();

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        end,
                        prep.client(),
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.CONFIRMED
                )
        );

        cancelAppointmentUseCase.cancelAppointment(appointment.getId(), prep.client().getId());
        Client client = clientRepository.findById(prep.client().getId())
                .orElse(null);

        appointment = appointmentRepository.findById(appointment.getId())
                .orElse(null);

        assertThat(appointment)
                .extracting(Appointment::getAppointmentStatus)
                .isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(client)
                .extracting(Client::getCanceledAppointments)
                .isEqualTo(1);
    }

    @Test
    void cancelAppointment_shouldThrowBusinessException_whenAppointmentNotFound() {
        PreparationData prep = getPreparationData();

        assertThrows(BusinessException.class,
                () -> cancelAppointmentUseCase.cancelAppointment(5712L, prep.client().getId()));
    }

    @Test
    void cancelAppointment_shouldThrowBusinessException_whenAppointmentDoesntBelongToClient() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS);
        PreparationData prep = getPreparationData();

        Client clientA = prep.client();
        Client clientB = clientRepository.save(TestClientFactory.standardForIt());

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        start.plus(1, ChronoUnit.HOURS),
                        clientA,
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.CONFIRMED
                )
        );
        assertThrows(BusinessException.class,
                () -> cancelAppointmentUseCase.cancelAppointment(appointment.getId(), clientB.getId()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void cancelAppointment_shouldUpdateOnlyOnce_whenThereIsARaceCondition() throws InterruptedException {
        PreparationData prep = getPreparationData();

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).minus(3, ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        end,
                        prep.client(),
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.CONFIRMED
                )
        );

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            Appointment finalAppointment = appointment;
            executorService.submit(() -> {
                try {
                    TenantContext.setTenant("tenant-test");
                    startLatch.await();
                    cancelAppointmentUseCase.cancelAppointment(finalAppointment.getId(), prep.client().getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    TenantContext.clear();
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        Client client = clientRepository.findById(prep.client().getId())
                .orElse(null);

        appointment = appointmentRepository.findById(appointment.getId())
                .orElse(null);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(exceptionCount.get()).isEqualTo(1);

        assertThat(appointment)
                .extracting(Appointment::getAppointmentStatus)
                .isEqualTo(AppointmentStatus.CANCELLED);

        assertThat(client)
                .extracting(Client::getCanceledAppointments)
                .isEqualTo(1);
    }

    @Test
    void cancelAppointment_shouldThrowBusinessException_whenAppointmentDateIsInThePast() {
        PreparationData prep = getPreparationData();

        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        end,
                        prep.client(),
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.CONFIRMED
                )
        );

        assertThrows(BusinessException.class,
                () -> cancelAppointmentUseCase.cancelAppointment(appointment.getId(), prep.client().getId()));
    }

    @Test
    void cancelAppointment_shouldThrowBusinessException_whenStatusIsAlreadyFinal() {
        PreparationData prep = getPreparationData();

        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        var appointment = appointmentRepository.save(
                TestAppointmentFactory.atSpecificTimeForIt(
                        start,
                        end,
                        prep.client(),
                        prep.professional(),
                        prep.service(),
                        AppointmentStatus.FINISHED
                )
        );
        assertThrows(BusinessException.class,
                () -> cancelAppointmentUseCase.cancelAppointment(appointment.getId(), prep.client().getId()));
    }
}