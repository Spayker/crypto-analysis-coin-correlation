package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.config.CandleGrabberConfig;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    private Map<String, TradeHistory> storage;

    @BeforeEach
    void setUp() {
        storage = new ConcurrentHashMap<>();

        when(tradeDataHistoryStorage.getCandleStickHistory()).thenReturn(storage);
        when(candleGrabberConfig.getHourIntervalType()).thenReturn("60");
    }

    @Test
    void shouldStoreTradeHistory_whenKlinesAreValid() {
        // given
        String pair = "BTCUSDT";
        Instant requestStart = Instant.parse("2024-01-01T00:00:00Z");

        CorrelationRequest request = mock(CorrelationRequest.class);
        when(request.getStartDateTime()).thenReturn(requestStart);

        Kline kline = mock(Kline.class);
        when(kline.getStartTime()).thenReturn(requestStart.plusSeconds(3600).toEpochMilli());

        when(byBitExchangeAdapter.getHourKlines(pair, "60", request))
                .thenReturn(List.of(kline));

        // when
        candleHistoryManager.initTradeHistory(pair, request);

        // then
        assertThat(storage).containsKey(pair);
        assertThat(storage.get(pair).getKlines()).hasSize(1);
    }

    @Test
    void shouldNotStoreTradeHistory_whenKlinesAreInvalid() {
        // given
        String pair = "ETHUSDT";
        CorrelationRequest request = mock(CorrelationRequest.class);

        Kline kline = mock(Kline.class);
        // invalid: same start time, not +3600
        when(byBitExchangeAdapter.getHourKlines(pair, "60", request))
                .thenReturn(List.of(kline));

        // mock isTradeHistoryValid to return false
        CandleHistoryManager managerSpy = spy(candleHistoryManager);
        doReturn(false).when(managerSpy).isTradeHistoryValid(request, List.of(kline));

        // when
        managerSpy.initTradeHistory(pair, request);

        // then
        assertThat(storage).doesNotContainKey(pair);
    }

    @Test
    void shouldRemoveExistingHistoryBeforeInsert() {
        // given
        String pair = "SOLUSDT";
        storage.put(pair, TradeHistory.builder().build());

        CorrelationRequest request = mock(CorrelationRequest.class);
        when(byBitExchangeAdapter.getHourKlines(any(), any(), any()))
                .thenReturn(List.of());

        // when
        candleHistoryManager.initTradeHistory(pair, request);

        // then
        assertThat(storage).doesNotContainKey(pair);
    }
}