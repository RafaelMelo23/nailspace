package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.domain.BusyIntervalService;
import com.rafael.agendanails.webapp.domain.model.Appointment;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.domain.repository.AppointmentRepository;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingAppointmentUseCase {

    private final AppointmentRepository repository;
    private final BookingContextLoader contextLoader;
    private final BookingValidator validator;
    private final BusyIntervalService busyIntervalService;

    @Transactional
    public void bookAppointment(
            AppointmentCreateDTO dto,
            UserPrincipal principal
    ) {
        BookingContext context = contextLoader.load(dto, principal);
        
        validator.validate(context, dto, principal);

        Appointment appointment = Appointment.create(
                dto,
                context.client(),
                context.professional(),
                context.mainService(),
                context.addOns(),
                context.salonProfile(),
                context.interval()
        );

        repository.save(appointment);
        repository.flush();

        busyIntervalService.evictCacheForAppointment(appointment, context.salonProfile().getZoneId());

        applyAppointmentConfirmationPolicy(context, appointment);
    }

    private void applyAppointmentConfirmationPolicy(BookingContext context,
                                                    Appointment appointment) {

        if (context.salonProfile().isAutoConfirmationAppointment()) {
            appointment.confirm();
            repository.save(appointment);
            busyIntervalService.evictCacheForAppointment(appointment, context.salonProfile().getZoneId());
        }
    }
}