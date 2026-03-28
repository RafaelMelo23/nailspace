package com.rafael.nailspro.webapp.shared.tenant;

public class TenantContext {

    private TenantContext() {

    }

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ignoreFilter = ThreadLocal.withInitial(() -> false);

    public static void clear() {
        currentTenant.remove();
        ignoreFilter.remove();
    }

    public static String getTenant() {
        return currentTenant.get();
    }

    public static void setTenant(String tenant) {
        currentTenant.set(tenant);
    }

    public static void setIgnoreFilter(boolean ignore) {
        ignoreFilter.set(ignore);
    }

    public static boolean isIgnoreFilter() {
        return Boolean.TRUE.equals(ignoreFilter.get());
    }
}
