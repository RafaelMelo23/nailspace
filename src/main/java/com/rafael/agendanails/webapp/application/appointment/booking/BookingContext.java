package com.rafael.agendanails.webapp.application.appointment.booking;

import com.rafael.agendanails.webapp.domain.model.*;

import java.util.List;

public record BookingContext(
    Client client,
    Professional professional,
    SalonService mainService,
    List<AppointmentAddOn> addOns,
    SalonProfile salonProfile,
    TimeInterval interval
) {}
