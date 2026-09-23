package com.asalkartechworks.daddysodahouse.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusControllerIntegrationTest {

    private final MockMvc mockMvc;

    @Autowired
    public StatusControllerIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    @DisplayName("Verify Spring context loads successfully")
    void contextLoads() {
        // Confirms Spring ApplicationContext starts with all foundation beans
    }

    @Test
    @DisplayName("GET /api/v1/public/status returns HTTP 200 and success=true")
    void getPublicStatus_shouldReturnHttp200AndExpectedJsonPayload() throws Exception {
        mockMvc.perform(get("/api/v1/public/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Daddy Soda House API is running"))
                .andExpect(jsonPath("$.data.application").value("Daddy Soda House Backend"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /actuator/health is publicly accessible without authentication")
    void getActuatorHealth_shouldReturnHttp200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Protected endpoint under /api/v1/** rejects unauthenticated requests with HTTP 401")
    void getProtectedEndpoint_withoutAuth_shouldReturnHttp401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/secured-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
