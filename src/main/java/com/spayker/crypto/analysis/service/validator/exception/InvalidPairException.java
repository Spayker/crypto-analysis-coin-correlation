package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class InvalidPairException extends BusinessRuleViolationException {

    public InvalidPairException(String pair, String stableCoin) {
        super(
                "INVALID_PAIR",
                String.format("Pair '%s' does not end with expected stable coin '%s'.", pair, stableCoin),
                Map.of("pair", pair, "stableCoin", stableCoin)
        );
    }
}
