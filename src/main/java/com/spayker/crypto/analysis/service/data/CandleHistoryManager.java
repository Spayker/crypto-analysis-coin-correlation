package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.config.CandleGrabberConfig;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

    private static final long INITIAL_BACKOFF_MILLIS = 500;
    private static final long MAX_BACKOFF_MILLIS = 10000;

    public CandleHistoryManager(@Autowired ByBitExchangeAdapter byBitExchangeAdapter,
                                @Autowired CandleGrabberConfig candleGrabberConfig,
                                @Autowired TradeDataHistoryStorage tradeDataHistoryStorage,
                                @Autowired RateLimiterRegistry registry) {
        this.byBitExchangeAdapter = byBitExchangeAdapter;
        this.candleGrabberConfig = candleGrabberConfig;
        this.tradeDataHistoryStorage = tradeDataHistoryStorage;
        this.rateLimiter = registry.rateLimiter("bybitLimiter");
    }

    public void initKLineHistoryByPairName(String targetCoin, List<String> pairNames, CorrelationRequest correlationRequestDto) {
        List<String> symbols = new ArrayList<>(pairNames);
        symbols.add(targetCoin + correlationRequestDto.getStableCoin());

        List<CompletableFuture<Void>> futures = symbols.stream()
                .map(symbol -> CompletableFuture.runAsync(() -> fetchHistoryWithRetry(symbol, correlationRequestDto), pool))
                .toList();

        futures.forEach(CompletableFuture::join);
    }

    private void fetchHistoryWithRetry(String symbol, CorrelationRequest correlationRequestDto) {
        int attempt = 1;
        while (true) {
            try {
                executeWithRateLimiter(symbol, correlationRequestDto);
                log.info("Successfully fetched history for symbol: {} after {} attempts", symbol, attempt);
                break;
            } catch (RequestNotPermitted ex) {
                handleRateLimitBackoff(symbol, attempt);
                attempt++;
            } catch (Exception ex) {
                handleGenericRetry(symbol, ex);
                attempt++;
            }
        }
    }

    private void executeWithRateLimiter(String symbol, CorrelationRequest correlationRequestDto) {
        Runnable decoratedRunnable = RateLimiter.decorateRunnable(rateLimiter, () -> initTradeHistory(symbol, correlationRequestDto));
        decoratedRunnable.run();
    }

    private void handleRateLimitBackoff(String symbol, int attempt) {
        long backoffMillis = Math.min(INITIAL_BACKOFF_MILLIS * (long) Math.pow(2, attempt - 1), MAX_BACKOFF_MILLIS);
        log.warn("Rate limit exceeded for symbol: {}. Attempt {}. Backing off {} ms.", symbol, attempt, backoffMillis);
        sleep(backoffMillis);
    }

    private void handleGenericRetry(String symbol, Exception ex) {
        log.error("Error fetching candle history for symbol: {}. Retrying...", symbol, ex);
        sleep(INITIAL_BACKOFF_MILLIS);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Sleep interrupted", ie);
        }
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

    boolean isTradeHistoryValid(CorrelationRequest correlationRequestDto, List<Kline> kLines) {
        if (kLines.isEmpty()) {
            return false;
        }
        Instant requestedStartDateTime = correlationRequestDto.getStartDateTime();
        Instant firstKlineStartTime = Instant.ofEpochMilli(kLines.getFirst().getStartTime());
        long timeDifference = Duration.between(requestedStartDateTime, firstKlineStartTime).toSeconds();
        return timeDifference >= 0 && timeDifference <= 3600;
    }
}