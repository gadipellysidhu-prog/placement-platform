package com.college.placement.shared.security;

import com.college.placement.shared.ratelimit.RateLimitFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
public class SecurityConfig {

    private static final AntPathRequestMatcher[] PUBLIC_MATCHERS = {
        new AntPathRequestMatcher("/auth/login"),
        new AntPathRequestMatcher("/auth/register"),
        new AntPathRequestMatcher("/auth/refresh"),
        new AntPathRequestMatcher("/auth/verify-email/request"),
        new AntPathRequestMatcher("/auth/verify-email/confirm"),
        new AntPathRequestMatcher("/auth/resend-verification"),
        new AntPathRequestMatcher("/auth/forgot-password"),
        new AntPathRequestMatcher("/auth/reset-password"),
        new AntPathRequestMatcher("/auth/accept-invitation"),
        new AntPathRequestMatcher("/actuator/health"),
        new AntPathRequestMatcher("/actuator/health/**"),
        new AntPathRequestMatcher("/swagger-ui/**"),
        new AntPathRequestMatcher("/swagger-ui.html"),
        new AntPathRequestMatcher("/v3/api-docs/**"),
        new AntPathRequestMatcher("/v3/api-docs")
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   RateLimitFilter rateLimitFilter,
                                                   SecurityProblemHandler problemHandler,
                                                   SecurityProperties securityProperties) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> {
                headers
                    .contentTypeOptions(c -> {})
                    .frameOptions(f -> f.deny())
                    .addHeaderWriter(new StaticHeadersWriter(
                        "Referrer-Policy", "strict-origin-when-cross-origin"))
                    .addHeaderWriter(new StaticHeadersWriter(
                        "Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()"))
                    .addHeaderWriter(new StaticHeadersWriter(
                        "Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'none'; object-src 'none'"));

                if (securityProperties.getHeaders().isHstsEnabled()) {
                    headers.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31_536_000));
                } else {
                    headers.httpStrictTransportSecurity(hsts -> hsts.disable());
                }
            })
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemHandler)
                .accessDeniedHandler(problemHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_MATCHERS).permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
            "ROLE_ADMIN > ROLE_PLACEMENT_OFFICER\nROLE_PLACEMENT_OFFICER > ROLE_STUDENT"
        );
    }

    @Bean
    static org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
            methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler handler =
                new org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setEnabled(false);
        return bean;
    }
}
