package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class UnsupportedCoinException
        extends BusinessRuleViolationException {

    public UnsupportedCoinException(String coin) {
        super(
                "UNSUPPORTED_COIN",
                "Coin is not supported",
                Map.of("coin", coin)
        );
    }
}

