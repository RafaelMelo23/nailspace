package com.rafael.nailspro.webapp.shared.tenant;

import jakarta.servlet.ServletRequest;

public interface TenantResolver {

    String resolve(ServletRequest servletRequest);
}
