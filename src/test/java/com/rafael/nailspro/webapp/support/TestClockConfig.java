package com.rafael.nailspro.webapp.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@TestConfiguration
public class TestClockConfig {
    public static final Instant FIXED_INSTANT = Instant.parse("2026-03-27T10:00:00Z");
    public static final ZoneId FIXED_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(FIXED_INSTANT, FIXED_ZONE);
    }
}
