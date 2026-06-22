package com.college.placement.shared.observability.metrics;

import com.college.placement.modules.auth.repository.AppUserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PlatformGauges {

    public PlatformGauges(AppUserRepository userRepository, MeterRegistry registry) {
        Gauge.builder("platform.users.total", userRepository, AppUserRepository::count)
                .description("Total registered users in the platform")
                .register(registry);
    }
}
