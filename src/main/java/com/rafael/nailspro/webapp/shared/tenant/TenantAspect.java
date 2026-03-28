package com.rafael.nailspro.webapp.shared.tenant;

import com.rafael.nailspro.webapp.infrastructure.exception.TenantNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class TenantAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Around("execution(* com.rafael.nailspro.webapp..*(..))")
    @Order(1)
    public Object handleTenancy(ProceedingJoinPoint pjp) throws Throwable {
        boolean hasIgnore = hasIgnoreAnnotation(pjp);
        boolean isRepo = isRepositoryCall(pjp);

        if (!isRepo && !hasIgnore) {
            return pjp.proceed();
        }

        if (hasIgnore) {
            boolean previous = TenantContext.isIgnoreFilter();
            TenantContext.setIgnoreFilter(true);
            log.trace("TenantAspect: [FLAG-SET] via annotation for {}", pjp.getSignature().toShortString());
            try {
                if (isRepo) {
                    return proceedWithoutFilter(pjp);
                }
                return pjp.proceed();
            } finally {
                TenantContext.setIgnoreFilter(previous);
                log.trace("TenantAspect: [FLAG-RESTORED] to {} for {}", previous, pjp.getSignature().toShortString());
            }
        }

        if (isRepo) {
            if (TenantContext.isIgnoreFilter()) {
                return proceedWithoutFilter(pjp);
            }
            return proceedWithFilter(pjp);
        }

        return pjp.proceed();
    }

    private Object proceedWithoutFilter(ProceedingJoinPoint pjp) throws Throwable {
        log.debug("TenantAspect: [BYPASS] filter for {}", pjp.getSignature().toShortString());
        Session session = entityManager.unwrap(Session.class);
        session.disableFilter("tenantFilter");
        return pjp.proceed();
    }

    private Object proceedWithFilter(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = TenantContext.getTenant();
        if (tenantId == null) {
            log.error("TenantAspect: [DENIED] access without tenant for {}", pjp.getSignature().toShortString());
            throw new TenantNotFoundException();
        }

        Session session = entityManager.unwrap(Session.class);
        Filter existing = session.getEnabledFilter("tenantFilter");
        boolean isNewlyEnabled = false;

        if (existing == null) {
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            isNewlyEnabled = true;
            log.debug("TenantAspect: [FILTER-ON] for tenant: [{}] during {}", tenantId, pjp.getSignature().toShortString());
        } else {
            existing.setParameter("tenantId", tenantId);
        }

        try {
            return pjp.proceed();
        } finally {
            if (isNewlyEnabled) {
                session.disableFilter("tenantFilter");
                log.debug("TenantAspect: [FILTER-OFF] for tenant: [{}] after {}", tenantId, pjp.getSignature().toShortString());
            }
        }
    }

    private boolean isRepositoryCall(ProceedingJoinPoint pjp) {
        String declaringType = pjp.getSignature().getDeclaringTypeName();
        return declaringType.contains(".domain.repository.") || 
               pjp.getTarget() instanceof org.springframework.data.repository.Repository;
    }

    private boolean hasIgnoreAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        return AnnotationUtils.findAnnotation(method, IgnoreTenantFilter.class) != null ||
               AnnotationUtils.findAnnotation(method.getDeclaringClass(), IgnoreTenantFilter.class) != null ||
               AnnotationUtils.findAnnotation(pjp.getTarget().getClass(), IgnoreTenantFilter.class) != null;
    }
}