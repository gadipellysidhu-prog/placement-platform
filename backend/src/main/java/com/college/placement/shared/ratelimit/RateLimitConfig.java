package com.college.placement.shared.ratelimit;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    /**
     * Prevent Spring Boot from auto-registering RateLimitFilter as a raw servlet filter.
     * It is added to the Spring Security filter chain by SecurityConfig instead,
     * which ensures it runs inside the security context with correct ordering.
     */
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
