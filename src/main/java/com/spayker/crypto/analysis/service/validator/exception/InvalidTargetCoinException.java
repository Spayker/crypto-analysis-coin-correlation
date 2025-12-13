package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class InvalidTargetCoinException extends BusinessRuleViolationException {

    public InvalidTargetCoinException(String message) {
        super("INVALID_TARGET_COIN", message);
    }

    public InvalidTargetCoinException(String targetCoin, String stableCoin) {
        super(
                "INVALID_TARGET_COIN",
                String.format("Target coin '%s' cannot be the same as stable coin '%s'.", targetCoin, stableCoin),
                Map.of("targetCoin", targetCoin, "stableCoin", stableCoin)
        );
    }
}
