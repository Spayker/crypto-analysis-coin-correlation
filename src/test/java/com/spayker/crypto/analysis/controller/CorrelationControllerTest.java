package com.spayker.crypto.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spayker.crypto.analysis.service.CorrelationManager;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationPair;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationResponse;
import com.spayker.crypto.analysis.service.validator.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CorrelationController.class)
//@Import(WebConfig.class) // Include your Instant converter
class CorrelationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CorrelationManager correlationManager;

    @Test
    void fetchCandleHistoryByPairName_shouldReturnOkAndCallManager() throws Exception {
        CorrelationResponse mockResponse = CorrelationResponse.builder()
                .targetCoinPercentChange("5.69624211988938")
                .correlationPairs(List.of(
                        new CorrelationPair("ALCHUSDT", "20"),
                        new CorrelationPair("FHEUSDT", "30")
                ))
                .build();

        Mockito.when(correlationManager.getCorrelation(anyString(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/v1/BTC/correlations")
                        .queryParam("symbols", "ALCHUSDT", "FHEUSDT")
                        .queryParam("startDateTime", "2025-12-01T00:00:00Z")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z")
                        .queryParam("stableCoin", "USDT"))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(mockResponse)));

        Mockito.verify(correlationManager, Mockito.times(1))
                .getCorrelation(anyString(), any(CorrelationRequest.class));
    }

    @Test
    void whenInvalidTargetCoin_thenConstraintViolationHandled() throws Exception {
        mockMvc.perform(get("/v1/btc123/correlations")
                        .queryParam("symbols", "ALCHUSDT")
                        .queryParam("startDateTime", "2025-12-01T00:00:00Z")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z")
                        .queryParam("stableCoin", "USDT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message")
                        .value("Request parameter validation failed"))
                .andExpect(jsonPath("$.details").exists());
    }

    @Test
    void whenMissingStableCoin_thenMissingParamHandled() throws Exception {
        mockMvc.perform(get("/v1/BTC/correlations")
                        .queryParam("symbols", "ALCHUSDT")
                        .queryParam("startDateTime", "2025-12-01T00:00:00Z")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("stableCoin is required"))
                .andExpect(jsonPath("$.details.parameter").value("stableCoin"));
    }

    @Test
    void whenMissingStartDateTime_thenMissingParamHandled() throws Exception {
        mockMvc.perform(get("/v1/BTC/correlations")
                        .queryParam("symbols", "ALCHUSDT")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z")
                        .queryParam("stableCoin", "USDT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("startDateTime is required"))
                .andExpect(jsonPath("$.details.parameter").value("startDateTime"));
    }

    @Test
    void whenBusinessRuleViolation_thenUnprocessableEntity() throws Exception {
        Mockito.when(correlationManager.getCorrelation(any(), any()))
                .thenThrow(new BusinessRuleViolationException(
                        "MY_RULE", "Rule violated", Map.of("field", "value")) {});

        mockMvc.perform(get("/v1/BTC/correlations")
                        .queryParam("symbols", "ALCHUSDT")
                        .queryParam("startDateTime", "2025-12-01T00:00:00Z")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z")
                        .queryParam("stableCoin", "USDT"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("MY_RULE"))
                .andExpect(jsonPath("$.message").value("Rule violated"))
                .andExpect(jsonPath("$.details.field").value("value"));
    }

    @Test
    void whenUnexpectedException_thenInternalServerError() throws Exception {
        Mockito.when(correlationManager.getCorrelation(any(), any()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/v1/BTC/correlations")
                        .queryParam("symbols", "ALCHUSDT")
                        .queryParam("startDateTime", "2025-12-01T00:00:00Z")
                        .queryParam("endDateTime", "2025-12-10T00:00:00Z")
                        .queryParam("stableCoin", "USDT"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

}

