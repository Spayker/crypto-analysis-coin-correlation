package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.config.CandleGrabberConfig;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandleHistoryManagerTest {

    @Mock
    private ByBitExchangeAdapter byBitExchangeAdapter;

    @Mock
    private CandleGrabberConfig candleGrabberConfig;

    @Mock
    private TradeDataHistoryStorage tradeDataHistoryStorage;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private CandleHistoryManager candleHistoryManager;

    private Map<String, TradeHistory> candleHistoryMap;

    private AutoCloseable autoCloseable;

    @BeforeEach
    void setup() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        // Real RateLimiterConfig
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(0))
                .build();

        rateLimiter = RateLimiter.of("bybit", config);

        // Mock registry to return real RateLimiter
        when(rateLimiterRegistry.rateLimiter("bybit")).thenReturn(rateLimiter);

        // Storage map
        candleHistoryMap = new ConcurrentHashMap<>();
        when(tradeDataHistoryStorage.getCandleStickHistory()).thenReturn(candleHistoryMap);

        // Config
        when(candleGrabberConfig.getHourIntervalType()).thenReturn("60");

        // Construct manager manually
        candleHistoryManager = new CandleHistoryManager(
                byBitExchangeAdapter,
                candleGrabberConfig,
                tradeDataHistoryStorage,
                rateLimiterRegistry
        );
    }

    @AfterEach
    public void postExecute() throws Exception {
        autoCloseable.close();
    }

    @Test
    void initCandleStickHistoryByPairName_shouldAppendTargetSymbolAndStoreResults() {
        // given
        List<String> initialPairs = List.of("FHEUSDT", "ETHUSDT");

        CorrelationRequest request = new CorrelationRequest(
                initialPairs,
                Instant.parse("2025-12-01T00:00:00Z"),
                Instant.parse("2025-12-05T00:00:00Z"),
                "USDT"
        );
        List<Kline> mockKlines = List.of(
                Kline.builder().startTime(100L).build(),
                Kline.builder().startTime(200L).build()
        );
        when(byBitExchangeAdapter.getHourKlines(anyString(), eq("60"), eq(request)))
                .thenReturn(mockKlines);

        // when
        candleHistoryManager.initCandleStickHistoryByPairName("BTC", initialPairs, request);

        // then
        assertEquals(3, candleHistoryMap.size());
        assertTrue(candleHistoryMap.containsKey("BTCUSDT"));
        assertTrue(candleHistoryMap.containsKey("ETHUSDT"));
        assertTrue(candleHistoryMap.containsKey("FHEUSDT"));
        verify(byBitExchangeAdapter, times(3)).getHourKlines(anyString(), eq("60"), eq(request));
    }

    @Test
    void initTradeHistory_shouldReplaceEntryAndStoreNewKlines() throws Exception {
        // given
        String symbol = "ALGOUSDT";
        candleHistoryMap.put(symbol, TradeHistory.builder()
                .klines(List.of(Kline.builder().startTime(10L).build()))
                .build());

        CorrelationRequest request = new CorrelationRequest(
                List.of(),
                Instant.now(),
                Instant.now().plusSeconds(300),
                "USDT"
        );

        List<Kline> mockKlines = List.of(Kline.builder().startTime(999L).build());
        when(byBitExchangeAdapter.getHourKlines(symbol, "60", request))
                .thenReturn(mockKlines);

        // when
        var method = CandleHistoryManager.class
                .getDeclaredMethod("initTradeHistory", String.class, CorrelationRequest.class);
        method.setAccessible(true);
        method.invoke(candleHistoryManager, symbol, request);

        // then
        TradeHistory history = candleHistoryMap.get(symbol);
        assertNotNull(history);
        assertEquals(1, history.getKlines().size());
        assertEquals(999L, history.getKlines().getFirst().getStartTime());
    }
}