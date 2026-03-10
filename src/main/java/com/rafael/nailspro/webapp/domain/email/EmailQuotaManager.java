package com.rafael.nailspro.webapp.domain.email;

public interface EmailQuotaManager {
    boolean isQuotaAvailable();
    void registerSuccessfulSend();
}
