package com.spayker.crypto.analysis.service.validator.exception;

import java.util.Map;

public class WindowExceedsRangeException
        extends BusinessRuleViolationException {

    public WindowExceedsRangeException(int window, long daysBetween) {
        super(
                "WINDOW_EXCEEDS_RANGE",
                "Window exceeds selected date range",
                Map.of(
                        "window", String.valueOf(window),
                        "daysBetween", String.valueOf(daysBetween)
                )
        );
    }
}