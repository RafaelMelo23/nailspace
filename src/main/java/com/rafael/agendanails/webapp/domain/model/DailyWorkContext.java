package com.rafael.agendanails.webapp.domain.model;

import java.time.LocalDate;

public record DailyWorkContext(LocalDate date, WorkSchedule schedule) {
}
