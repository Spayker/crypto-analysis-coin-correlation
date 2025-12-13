package com.spayker.crypto.analysis.service.validator;

import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.validator.exception.EmptyPairListException;
import com.spayker.crypto.analysis.service.validator.exception.InvalidPairException;
import com.spayker.crypto.analysis.service.validator.exception.InvalidTargetCoinException;
import com.spayker.crypto.analysis.service.validator.exception.UnsupportedCoinException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BusinessRuleValidatorTest {

    @Test
    void validateRequest_withValidParams_doesNotThrow() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("BTCUSDT"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        assertDoesNotThrow(() ->
                BusinessRuleValidator.validateRequest("BTC", request));
    }

    @Test
    void validateRequest_whenTargetCoinIsNull_throwsInvalidTargetCoinException() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("ETHUSDT"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        assertThrows(InvalidTargetCoinException.class, () ->
                BusinessRuleValidator.validateRequest(null, request));
    }

    @Test
    void validateRequest_whenStableCoinIsNull_throwsUnsupportedCoinException() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("ETHUSDT"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                null
        );

        assertThrows(UnsupportedCoinException.class, () ->
                BusinessRuleValidator.validateRequest("ETH", request));
    }

    @Test
    void validateRequest_whenTargetEqualsStable_throwsInvalidTargetCoinException() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("ETHETH"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "ETH"
        );

        assertThrows(InvalidTargetCoinException.class, () ->
                BusinessRuleValidator.validateRequest("ETH", request));
    }

    @Test
    void validateResolvedPairs_withNonEmptyPairs_doesNotThrow() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("BTCUSDT"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        List<String> pairs = List.of("BTCUSDT");
        assertDoesNotThrow(() ->
                BusinessRuleValidator.validateResolvedPairs("BTC", request, pairs));
    }

    @Test
    void validateResolvedPairs_whenListIsEmpty_throwsEmptyPairListException() {
        CorrelationRequest request = new CorrelationRequest(
                Collections.emptyList(),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        assertThrows(EmptyPairListException.class, () ->
                BusinessRuleValidator.validateResolvedPairs("BTC", request, Collections.emptyList()));
    }

    @Test
    void validateResolvedPairs_whenPairDoesNotMatchStableCoin_throwsInvalidPairException() {
        CorrelationRequest request = new CorrelationRequest(
                List.of("BTCETH"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        List<String> pairs = List.of("BTCETH");
        assertThrows(InvalidPairException.class, () ->
                BusinessRuleValidator.validateResolvedPairs("BTC", request, pairs));
    }

    @Test
    void validateResolvedPairs_multipleInvalidPairs_throwsInvalidPairException() {
        CorrelationRequest request = new CorrelationRequest(
                Arrays.asList("XRPBTC", "LTCETH"),
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        List<String> pairs = Arrays.asList("XRPBTC", "LTCETH");
        assertThrows(InvalidPairException.class, () ->
                BusinessRuleValidator.validateResolvedPairs("BTC", request, pairs));
    }
}