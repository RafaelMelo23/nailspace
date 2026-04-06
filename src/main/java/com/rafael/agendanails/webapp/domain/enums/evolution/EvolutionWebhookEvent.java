package com.rafael.agendanails.webapp.domain.enums.evolution;

import com.fasterxml.jackson.annotation.JsonAlias;

public enum EvolutionWebhookEvent {
    @JsonAlias("qrcode.updated")
    QRCODE_UPDATED,

    @JsonAlias("connection.update")
    CONNECTION_UPDATE,

    @JsonAlias("send.message")
    SEND_MESSAGE,

    @JsonAlias("messages.update")
    MESSAGES_UPDATE;
}