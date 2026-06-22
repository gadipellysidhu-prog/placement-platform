package com.college.placement.observability;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    classes = com.college.placement.Application.class,
    properties = {
        "management.endpoints.web.exposure.include=*",
        "management.prometheus.metrics.export.enabled=true"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealth_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void actuatorMetrics_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_isPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"));
    }

    @Test
    void requestCorrelationFilter_addsTraceIdHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-ID"))
                .andExpect(header().exists("X-Request-ID"));
    }

    @Test
    void requestCorrelationFilter_propagatesProvidedRequestId() throws Exception {
        mockMvc.perform(get("/actuator/health").header("X-Request-ID", "my-custom-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "my-custom-id"));
    }

    @Test
    void prometheusEndpoint_containsAuthMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auth_login_success_total")));
    }

    @Test
    void prometheusEndpoint_containsOutboxMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("outbox_pending_count")));
    }
}
