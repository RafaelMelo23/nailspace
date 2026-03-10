package com.rafael.nailspro.webapp.domain.email;

public interface EmailNotifier {

    void send(EmailMessage emailMessage);
}