package com.spayker.crypto.analysis.service.dto.correlation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationResponse {

    private String targetCoinPercentChange;
    private List<CorrelationPair> correlationPairs;

}
