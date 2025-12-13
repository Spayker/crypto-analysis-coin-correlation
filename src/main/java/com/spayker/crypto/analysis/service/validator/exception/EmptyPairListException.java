package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class EmptyPairListException extends BusinessRuleViolationException {

    public EmptyPairListException(String stableCoin) {
        super(
                "EMPTY_PAIR_LIST",
                "No trading pairs found for stable coin",
                Map.of("stableCoin", stableCoin)
        );
    }
}
