package com.rafael.nailspro.webapp.shared.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* com.rafael.nailspro.webapp.domain.repository..*(..)) && " +
            "!execution(* com.rafael.nailspro.webapp.domain.repository..*.findByEmailIgnoreCase(..))")
    public Object invokeWithTenantFilter(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = TenantContext.getTenant();

        if (tenantId == null) {
            return pjp.proceed();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter existing = session.getEnabledFilter("tenantFilter");
        boolean isEnabled = false;

        if (existing == null) {
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId);
            isEnabled = true;

            log.debug("Tenant filter enabled for tenant: [{}]", tenantId);
        }

        try {
            return pjp.proceed();

        } finally {
            if (isEnabled) {
                session.disableFilter("tenantFilter");
                log.debug("Tenant filter disabled for tenant: [{}]", tenantId);
            }
        }
    }
}