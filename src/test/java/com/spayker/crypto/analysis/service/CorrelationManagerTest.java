package com.spayker.crypto.analysis.service;

import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.data.ByBitExchangeAdapter;
import com.spayker.crypto.analysis.service.data.CandleHistoryManager;
import com.spayker.crypto.analysis.service.data.TradeDataHistoryStorage;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationManagerTest {

    @Mock
    private ByBitExchangeAdapter byBitExchangeAdapter;

    @Mock
    private TradeDataHistoryStorage tradeDataHistoryStorage;

    @Mock
    private CandleHistoryManager candleHistoryManager;

    @InjectMocks
    private CorrelationManager correlationManager;

    private Map<String, TradeHistory> candleHistoryMap;

    private AutoCloseable autoCloseable;

    @BeforeEach
    void setup() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        candleHistoryMap = new HashMap<>();
        when(tradeDataHistoryStorage.getCandleStickHistory()).thenReturn(candleHistoryMap);
    }

    @AfterEach
    public void postExecute() throws Exception {
        autoCloseable.close();
    }

    @Test
    void getCorrelation_shouldUseProvidedSymbols() {
        // given
        List<String> symbols = List.of("ETHUSDT", "ALGOUSDT");
        CorrelationRequest request = new CorrelationRequest(
                symbols,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );
        List<Kline> btcKlines = List.of(
                Kline.builder().openPrice(100).highPrice(150).lowPrice(90).closePrice(120).build()
        );
        candleHistoryMap.put("BTCUSDT", TradeHistory.builder().klines(btcKlines).build());
        List<Kline> ethKlines = List.of(
                Kline.builder().openPrice(50).highPrice(75).lowPrice(45).closePrice(60).build()
        );
        List<Kline> algoKlines = List.of(
                Kline.builder().openPrice(10).highPrice(20).lowPrice(8).closePrice(15).build()
        );

        candleHistoryMap.put("ETHUSDT", TradeHistory.builder().klines(ethKlines).build());
        candleHistoryMap.put("ALGOUSDT", TradeHistory.builder().klines(algoKlines).build());
        // when
        CorrelationResponse response = correlationManager.getCorrelation("BTC", request);

        // then
        verify(candleHistoryManager, times(1)).initCandleStickHistoryByPairName("BTC", symbols, request);
        assertNotNull(response);
        assertEquals("66.66666666666666", response.getTargetCoinPercentChange());
        assertEquals(2, response.getCorrelationPairs().size());
        assertTrue(response.getCorrelationPairs().stream().anyMatch(p -> p.getPairName().equals("ETH")));
        assertTrue(response.getCorrelationPairs().stream().anyMatch(p -> p.getPairName().equals("ALGO")));
    }

    @Test
    void getCorrelation_shouldFetchSymbolsWhenNull() {
        // given
        CorrelationRequest request = new CorrelationRequest(
                null,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "USDT"
        );

        List<String> allPairs = List.of("BTCUSDT", "ETHUSDT", "ALGOUSDT", "DOGEUSDT");
        when(byBitExchangeAdapter.getAllPairNames()).thenReturn(allPairs);
        List<Kline> btcKlines = List.of(
                Kline.builder().openPrice(100).highPrice(150).lowPrice(90).closePrice(120).build()
        );
        candleHistoryMap.put("BTCUSDT", TradeHistory.builder().klines(btcKlines).build());
        for (String pair : allPairs) {
            if (!pair.equals("BTCUSDT")) {
                candleHistoryMap.put(pair, TradeHistory.builder()
                        .klines(List.of(Kline.builder().openPrice(10).highPrice(20).lowPrice(5).closePrice(15).build()))
                        .build()
                );
            }
        }

        // when
        CorrelationResponse response = correlationManager.getCorrelation("BTC", request);

        // then
        List<String> expectedPairs = Stream.of("BTCUSDT", "ETHUSDT", "ALGOUSDT", "DOGEUSDT").filter(p -> p.endsWith("USDT")).toList();
        verify(candleHistoryManager, times(1)).initCandleStickHistoryByPairName("BTC", expectedPairs, request);
        assertNotNull(response);
        assertEquals("66.66666666666666", response.getTargetCoinPercentChange());
        assertEquals(4, response.getCorrelationPairs().size());
    }
}