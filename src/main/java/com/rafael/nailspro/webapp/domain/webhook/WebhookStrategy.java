package com.rafael.nailspro.webapp.domain.webhook;

public interface WebhookStrategy {

    void process(Object payload);

    String getSupportedTypeEvent();
}
