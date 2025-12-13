package com.spayker.crypto.analysis.controller;

import com.spayker.crypto.analysis.service.CorrelationManager;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("v1")
public class CorrelationController {

    private final CorrelationManager correlationManager;

    @GetMapping("/{targetCoin}/correlations")
    public ResponseEntity<?> fetchCandleHistoryByPairName(
            @PathVariable @Pattern(regexp = "[A-Z]{3,10}") String targetCoin,
            @RequestParam List<String> symbols,
            @RequestParam String startDateTime,
            @RequestParam String endDateTime,
            @RequestParam String stableCoin) {

        Instant start = Instant.parse(startDateTime);
        Instant end   = Instant.parse(endDateTime);
        CorrelationRequest request = new CorrelationRequest(symbols, start, end, stableCoin);

        return ResponseEntity.ok(correlationManager.getCorrelation(targetCoin, request));
    }
}