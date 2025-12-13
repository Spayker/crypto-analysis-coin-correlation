package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.dao.feign.bybit.PublicApiClient;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.KlineResult;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.Pair;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.PairListResult;
import feign.RetryableException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ByBitExchangeAdapter {

    private final PublicApiClient publicApiClient;
    private static final String SPOT_TRADE_CATEGORY = "spot";

    public List<String> getAllPairNames() {
        try {
            ExchangeResponse<PairListResult> pairNamesResponse = publicApiClient.getPairNames(SPOT_TRADE_CATEGORY);
            PairListResult pairListResult = pairNamesResponse.getResult();
            if (pairListResult != null) {
                return convertToPairs(pairListResult.getPairs());
            }
        } catch (RetryableException rE) {
            log.error(rE.getMessage());
        }
        return null;
    }

    List<Kline> getHourKlines(String pairName, String interval, CorrelationRequest correlationRequestDto) {
        Instant startDateTime = correlationRequestDto.getStartDateTime();
        Instant endDateTime = correlationRequestDto.getEndDateTime();
        List<ExchangeResponse<KlineResult>> hourKLineResult = new ArrayList<>();
        Instant chunkStart = startDateTime;

        while (chunkStart.isBefore(endDateTime)) {
            Instant chunkEnd = chunkStart.plus(Duration.ofHours(1000));
            // Don't go past the real endDateTime
            if (chunkEnd.isAfter(endDateTime)) {
                chunkEnd = endDateTime;
            }
            long chunkHours = Duration.between(chunkStart, chunkEnd).toHours();
            ExchangeResponse<KlineResult> partialResult =
                    publicApiClient.getCandlesHistoryByDate(
                            SPOT_TRADE_CATEGORY,
                            pairName,
                            interval,
                            String.valueOf(chunkHours),
                            String.valueOf(chunkStart.toEpochMilli()),
                            String.valueOf(chunkEnd.toEpochMilli())
                    );
            hourKLineResult.add(partialResult);
            // Move to the next segment
            chunkStart = chunkEnd;
        }
        return hourKLineResult.stream()
                .map(r -> r.getResult().getKlines())
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Kline::getStartTime))
                .toList();
    }

    private List<String> convertToPairs(List<Pair> pairs) {
        return pairs.stream()
                .map(Pair::getSymbol)
                .toList();
    }
}