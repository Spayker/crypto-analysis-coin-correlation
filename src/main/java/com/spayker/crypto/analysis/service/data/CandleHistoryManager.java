package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.config.CandleGrabberConfig;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class CandleHistoryManager {

    private final ByBitExchangeAdapter byBitExchangeAdapter;
    private final CandleGrabberConfig candleGrabberConfig;
    private final TradeDataHistoryStorage tradeDataHistoryStorage;
    private final RateLimiter rateLimiter;

    private final ExecutorService pool = Executors.newFixedThreadPool(40);

    public CandleHistoryManager(@Autowired ByBitExchangeAdapter byBitExchangeAdapter,
                                @Autowired CandleGrabberConfig candleGrabberConfig,
                                @Autowired TradeDataHistoryStorage tradeDataHistoryStorage,
                                @Autowired RateLimiterRegistry registry) {
        this.byBitExchangeAdapter = byBitExchangeAdapter;
        this.candleGrabberConfig = candleGrabberConfig;
        this.tradeDataHistoryStorage = tradeDataHistoryStorage;
        this.rateLimiter = registry.rateLimiter("bybit");
    }

    public void initCandleStickHistoryByPairName(String targetCoin, List<String> pairNames, CorrelationRequest correlationRequestDto) {
        List<String> allSymbols = new ArrayList<>(pairNames);
        allSymbols.add(targetCoin + correlationRequestDto.getStableCoin());
        allSymbols.stream()
                .map(pair -> CompletableFuture.runAsync(
                        () -> initCandleStickHistoryRateLimited(pair, correlationRequestDto), pool))
                .forEach(CompletableFuture::join);
    }

    private void initCandleStickHistoryRateLimited(String pairName, CorrelationRequest correlationRequestDto) {
        RateLimiter.waitForPermission(rateLimiter);
        initTradeHistory(pairName, correlationRequestDto);
    }

    void initTradeHistory(String pairName, CorrelationRequest correlationRequestDto) {
        Map<String, TradeHistory> candleStickHistory = tradeDataHistoryStorage.getCandleStickHistory();
        candleStickHistory.remove(pairName);
        List<Kline> kLines = byBitExchangeAdapter.getHourKlines(pairName,
                candleGrabberConfig.getHourIntervalType(), correlationRequestDto);
        if (isTradeHistoryValid(correlationRequestDto, kLines)) {
            candleStickHistory.put(pairName, TradeHistory.builder()
                            .klines(kLines)
                            .build()
            );
        }
    }

    private boolean isTradeHistoryValid(CorrelationRequest correlationRequestDto, List<Kline> kLines) {
        if (kLines.isEmpty()) {
            return false;
        }
        Instant requestedStartDateTime = correlationRequestDto.getStartDateTime();
        Instant firstKlineStartTime = Instant.ofEpochMilli(kLines.getFirst().getStartTime());
        return Duration.between(requestedStartDateTime, firstKlineStartTime).toSeconds() == 3600;
    }
}