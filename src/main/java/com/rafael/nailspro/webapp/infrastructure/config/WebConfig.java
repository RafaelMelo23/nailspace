package com.rafael.nailspro.webapp.infrastructure.config;

import com.rafael.nailspro.webapp.infrastructure.security.interceptor.SalonMaintenanceInterceptor;
import com.rafael.nailspro.webapp.infrastructure.security.interceptor.TenantStatusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SalonMaintenanceInterceptor salonMaintenanceInterceptor;
    private final TenantStatusInterceptor tenantStatusInterceptor;

    //todo: currently disabled for dev
    //@Override
    //public void addInterceptors(InterceptorRegistry registry) {
    //    registry.addInterceptor(salonMaintenanceInterceptor);
    //    registry.addInterceptor(tenantStatusFilter);
    //}
}
