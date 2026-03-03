package com.rafael.nailspro.webapp.infrastructure.config;

import com.rafael.nailspro.webapp.infrastructure.security.filter.SalonMaintenanceInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SalonMaintenanceInterceptor salonMaintenanceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(salonMaintenanceInterceptor);
    }
}
