package com.rafael.agendanails.webapp.infrastructure.config;

import com.rafael.agendanails.webapp.infrastructure.security.interceptor.EvolutionApiInterceptor;
import com.rafael.agendanails.webapp.infrastructure.security.interceptor.SalonMaintenanceInterceptor;
import com.rafael.agendanails.webapp.infrastructure.security.interceptor.TenantStatusInterceptor;
import com.rafael.agendanails.webapp.infrastructure.security.interceptor.UserStatusInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload.dir}")
    private String uploadDir;

    private final SalonMaintenanceInterceptor salonMaintenanceInterceptor;
    private final TenantStatusInterceptor tenantStatusInterceptor;
    private final EvolutionApiInterceptor evolutionApiInterceptor;
    private final UserStatusInterceptor userStatusInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(salonMaintenanceInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/webhook/**", "/api/v1/admin/**", "/api/internal/**");

        registry.addInterceptor(tenantStatusInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/webhook/**", "/api/v1/admin/**", "/api/internal/**");

        registry.addInterceptor(userStatusInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/webhook/**", "/api/internal/**");

        registry.addInterceptor(evolutionApiInterceptor)
                .addPathPatterns("/api/v1/webhook/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
