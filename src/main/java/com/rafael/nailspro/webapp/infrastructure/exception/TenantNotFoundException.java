package com.rafael.nailspro.webapp.infrastructure.exception;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String message) {
        super(message);
    }

    public TenantNotFoundException() {
        super("Operação não permitida: ID do estabelecimento (tenant) não definido.");
    }
}
