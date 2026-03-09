package com.rafael.nailspro.webapp.domain.whatsapp;

import com.rafael.nailspro.webapp.domain.enums.evolution.EvolutionMessageStatus;
import lombok.Builder;

@Builder
public record SentMessageResult(String messageId,
                                EvolutionMessageStatus status) {
}
