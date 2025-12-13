package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class TargetNotInPairsException
        extends BusinessRuleViolationException {

    public TargetNotInPairsException(String targetCoin) {
        super(
                "TARGET_NOT_IN_PAIRS",
                "Resolved pairs do not include target coin",
                Map.of("targetCoin", targetCoin)
        );
    }
}
