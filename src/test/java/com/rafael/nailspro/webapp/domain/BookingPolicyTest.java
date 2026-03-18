package com.rafael.nailspro.webapp.domain;

import com.rafael.nailspro.webapp.domain.model.*;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.booking.AppointmentTimeWindow;
import com.rafael.nailspro.webapp.infrastructure.exception.BusinessException;
import com.rafael.nailspro.webapp.support.factory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BookingPolicyTest {

    @InjectMocks
    private BookingPolicy bookingPolicy;

    @Test
    void resolveAllowedWindowDays_returnsStandardDefault() {
        int result = bookingPolicy.resolveAllowedWindowDays(false, false, null, null);
        assertEquals(7, result);
    }

    @Test
    void resolveAllowedWindowDays_handlesNullBooleans() {
        int result = bookingPolicy.resolveAllowedWindowDays(null, null, 30, 10);
        assertEquals(10, result);
    }

    @Test
    void resolveAllowedWindowDays_returnsStandardWhenPrioritizeIsTrueButNotLoyal() {
        int result = bookingPolicy.resolveAllowedWindowDays(true, null, 30, 10);
        assertEquals(10, result);
    }

    @Test
    void resolveAllowedWindowDays_returnsLoyalDefault() {
        int result = bookingPolicy.resolveAllowedWindowDays(true, true, null, null);
        assertEquals(30, result);
    }

    @Test
    void validateBookingHorizon_doesNotThrowWhenInsideWindow() {
        LocalDate today = LocalDate.now();
        LocalDate requestedDate = today.plusDays(3);

        assertDoesNotThrow(() -> bookingPolicy.validateBookingHorizon(requestedDate, 7, today));
    }

    @Test
    void validateBookingHorizon_throwsExceptionWhenOutsideWindow() {
        LocalDate today = LocalDate.now();
        LocalDate requestedDate = today.plusDays(8);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookingPolicy.validateBookingHorizon(requestedDate, 7, today));

        assertEquals("Sua janela de agendamento não é preferencial, aguarde alguns dias para reservar esta data.", exception.getMessage());
    }

    @Test
    void buildWindow_createsCorrectWindow() {
        LocalDate start = LocalDate.now();
        AppointmentTimeWindow window = bookingPolicy.buildWindow(start, 7);

        assertEquals(start, window.start());
        assertEquals(start.plusDays(7), window.end());
    }

    @Test
    void determineStartDate_returnsToday_WhenMaintenanceIntervalIsNull() {
        Client client = TestUserFactory.client();
        SalonService salonService = TestSalonServiceFactory.standardWithoutMaintenanceInterval();

        Appointment appointment = TestAppointmentFactory.standard(
                client,
                TestProfessionalFactory.standard(),
                salonService
        );

        LocalDate today = LocalDate.now();

        LocalDate startDate = bookingPolicy.determineStartDate(List.of(salonService), appointment);

        assertEquals(today, startDate);
    }

    @Test
    void determineStartDate_returnsToday_WhenMaintenanceIntervalAndLastAppointmentAreNull() {
        SalonService salonService = TestSalonServiceFactory.standardWithoutMaintenanceInterval();

        LocalDate bookingPolicyStartDate = bookingPolicy.determineStartDate(List.of(salonService), null);

        LocalDate today = LocalDate.now();
        assertEquals(today, bookingPolicyStartDate);
    }

    @Test
    void determineStartDate_returnsTodayWhenLastAppointmentIsEmpty() {
        SalonService salonService = TestSalonServiceFactory.standardWithoutMaintenanceInterval();

        LocalDate today = LocalDate.now();

        LocalDate startDate = bookingPolicy.determineStartDate(List.of(salonService), null);

        assertEquals(today, startDate);
    }

    @Test
    void determineStartDate_calculatesCorrectDateWithMaintenanceInterval() {
        Client client = TestUserFactory.client();
        SalonService salonService = TestSalonServiceFactory.withMaintenanceInterval(20);

        Appointment appointment = TestAppointmentFactory.standard(
                client,
                TestProfessionalFactory.standard(),
                salonService
        );
        LocalDate bookingPolicyStartDate = bookingPolicy.determineStartDate(List.of(salonService), appointment);

        LocalDate appointmentDate = LocalDate.ofInstant(appointment.getEndDate(), appointment.getSalonZoneId());
        LocalDate expectedDate = appointmentDate.plusDays(salonService.getMaintenanceIntervalDays() - 3);

        assertEquals(expectedDate, bookingPolicyStartDate);
    }

    @Test
    void calculateEarliestRecommendedDate_returnsNowIfAppointmentNull() {
        Instant before = Instant.now();
        Instant recommendedDate = bookingPolicy.calculateEarliestRecommendedDate(null);
        Instant after = Instant.now();

        assertTrue(
                !recommendedDate.isBefore(before) &&
                        !recommendedDate.isAfter(after)
        );
    }

    @Test
    void calculateEarliestRecommendedDate_returnsMainServiceMaintenanceInterval() {
        Client client = TestUserFactory.client();
        Professional professional = TestProfessionalFactory.standard();
        SalonService salonService = TestSalonServiceFactory.withMaintenanceInterval(20);
        AppointmentAddOn addOn = TestAppointmentAddOnFactory.standard(TestSalonServiceFactory.addOnWithMaintenanceInterval(15));

        Appointment appointment = TestAppointmentFactory.withAddOns(
                client,
                professional,
                salonService,
                List.of(addOn)
        );

        Instant recommendedDate = bookingPolicy.calculateEarliestRecommendedDate(appointment);
        Instant expectedDate = appointment.getEndDate().plus(salonService.getMaintenanceIntervalDays(), ChronoUnit.DAYS);

        assertEquals(expectedDate, recommendedDate);
    }

    @Test
    void calculateEarliestRecommendedDate_returnsAddOnMaintenanceInterval() {
        Client client = TestUserFactory.client();
        Professional professional = TestProfessionalFactory.standard();
        SalonService salonService = TestSalonServiceFactory.withMaintenanceInterval(20);
        AppointmentAddOn addOn = TestAppointmentAddOnFactory.standard(TestSalonServiceFactory.addOnWithMaintenanceInterval(25));

        Appointment appointment = TestAppointmentFactory.withAddOns(
                client,
                professional,
                salonService,
                List.of(addOn)
        );

        Instant recommendedDate = bookingPolicy.calculateEarliestRecommendedDate(appointment);
        Instant expectedDate = appointment.getEndDate().plus(addOn.getService().getMaintenanceIntervalDays(), ChronoUnit.DAYS);

        assertEquals(expectedDate, recommendedDate);
    }

    @Test
    void calculateEarliestRecommendedDate_returnsNowIfMaintenanceIntervalIsNull() {
        Client client = TestUserFactory.client();
        Professional professional = TestProfessionalFactory.standard();
        SalonService salonService = TestSalonServiceFactory.standardWithoutMaintenanceInterval();
        AppointmentAddOn addOn = TestAppointmentAddOnFactory.standard(TestSalonServiceFactory.addOnWithoutMaintenanceInterval());

        Appointment appointment = TestAppointmentFactory.withAddOns(
                client,
                professional,
                salonService,
                List.of(addOn)
        );

        Instant before = Instant.now();
        Instant recommendedDate = bookingPolicy.calculateEarliestRecommendedDate(appointment);
        Instant after = Instant.now();

        assertTrue(
                !recommendedDate.isBefore(before) &&
                        !recommendedDate.isAfter(after)
        );
    }
}