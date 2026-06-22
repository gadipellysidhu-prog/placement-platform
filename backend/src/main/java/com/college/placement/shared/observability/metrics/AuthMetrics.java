package com.college.placement.shared.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter registerSuccessCounter;
    private final Timer loginTimer;
    private final Timer registerTimer;
    private final MeterRegistry registry;

    public AuthMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.loginSuccessCounter = Counter.builder("auth.login.success.total")
                .description("Total successful login attempts")
                .register(registry);
        this.loginFailureCounter = Counter.builder("auth.login.failure.total")
                .description("Total failed login attempts")
                .register(registry);
        this.registerSuccessCounter = Counter.builder("auth.register.success.total")
                .description("Total successful user registrations")
                .register(registry);
        this.loginTimer = Timer.builder("auth.login.duration")
                .description("Login request processing time")
                .register(registry);
        this.registerTimer = Timer.builder("auth.register.duration")
                .description("Register request processing time")
                .register(registry);
    }

    public void recordLoginSuccess()    { loginSuccessCounter.increment(); }
    public void recordLoginFailure()    { loginFailureCounter.increment(); }
    public void recordRegisterSuccess() { registerSuccessCounter.increment(); }

    public Timer.Sample startSample()                      { return Timer.start(registry); }
    public void stopLoginTimer(Timer.Sample sample)        { sample.stop(loginTimer); }
    public void stopRegisterTimer(Timer.Sample sample)     { sample.stop(registerTimer); }
}
