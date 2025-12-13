package com.spayker.crypto.analysis.service.validator.exception;

import java.time.LocalDate;
import java.util.Map;

public class InvalidDateRangeException
        extends BusinessRuleViolationException {

    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super(
                "INVALID_DATE_RANGE",
                "From date must be before to date",
                Map.of(
                        "from", from.toString(),
                        "to", to.toString()
                )
        );
    }
}

