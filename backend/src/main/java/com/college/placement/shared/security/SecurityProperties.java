package com.college.placement.shared.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {

    private RateLimitProperties rateLimit = new RateLimitProperties();
    private LockoutProperties lockout = new LockoutProperties();
    private HeaderProperties headers = new HeaderProperties();

    @Getter
    @Setter
    public static class RateLimitProperties {
        private boolean enabled = true;
        private int loginCapacity = 5;
        private int registerCapacity = 3;
        private int refreshCapacity = 20;
        private int generalCapacity = 100;
    }

    @Getter
    @Setter
    public static class LockoutProperties {
        private int maxFailures = 5;
        private int lockDurationMinutes = 15;
    }

    @Getter
    @Setter
    public static class HeaderProperties {
        private boolean hstsEnabled = false;
    }
}
