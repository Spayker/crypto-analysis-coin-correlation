package com.spayker.crypto.analysis.service.validator;

import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.validator.exception.*;

import java.util.List;

public class BusinessRuleValidator {

    private BusinessRuleValidator() {}

    public static void validateRequest(String targetCoin, CorrelationRequest request) {
        if (targetCoin == null || targetCoin.isBlank()) {
            throw new InvalidTargetCoinException("Target coin is required");
        }

        if (request.getStableCoin() == null || request.getStableCoin().isBlank()) {
            throw new UnsupportedCoinException("Stable coin is required");
        }

        if (targetCoin.equalsIgnoreCase(request.getStableCoin())) {
            throw new InvalidTargetCoinException("Target and stable coin cannot be the same");
        }
    }

    public static void validateResolvedPairs(CorrelationRequest request, List<String> resolvedPairs) {
        if (resolvedPairs.isEmpty()) {
            throw new EmptyPairListException(request.getStableCoin());
        }

        // Validate each resolved pair
        resolvedPairs.forEach(pair -> validatePair(pair, request.getStableCoin()));
    }

    private static void validatePair(String pair, String stableCoin) {
        if (!pair.endsWith(stableCoin)) {
            throw new InvalidPairException(pair, stableCoin);
        }
    }


}
