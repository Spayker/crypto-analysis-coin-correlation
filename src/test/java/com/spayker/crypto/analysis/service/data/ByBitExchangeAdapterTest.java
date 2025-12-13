package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.dao.feign.bybit.PublicApiClient;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.KlineResult;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.Pair;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.PairListResult;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ByBitExchangeAdapterTest {

    @Mock
    private PublicApiClient publicApiClient;

    @InjectMocks
    private ByBitExchangeAdapter byBitExchangeAdapter;

    private AutoCloseable autoCloseable;

    @BeforeEach
    public void setup() {
        autoCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void postExecute() throws Exception {
        autoCloseable.close();
    }

    @Test
    void getAllPairNames_shouldReturnListOfSymbols_WhenResultPresent() {
        // Given: create test Pair objects
        Pair pair1 = Pair.builder()
                .symbol("BTCUSDT")
                .baseCoin("BTC")
                .quoteCoin("USDT")
                .innovation("0")
                .status("Trading")
                .marginTrading("None")
                .stTag("0")
                .build();


        Pair pair2 = Pair.builder()
                .symbol("ETHUSDT")
                .baseCoin("ETH")
                .quoteCoin("USDT")
                .innovation("0")
                .status("Trading")
                .marginTrading("None")
                .stTag("0")
                .build();

        PairListResult pairListResult = new PairListResult(
                "spot",
                List.of(pair1, pair2)
        );

        ExchangeResponse<PairListResult> response = new ExchangeResponse<>(100, "111", pairListResult, "1000");
        when(publicApiClient.getPairNames("spot")).thenReturn(response);

        // When
        List<String> result = byBitExchangeAdapter.getAllPairNames();

        // Then
        assertNotNull(result);
        assertEquals(List.of("BTCUSDT", "ETHUSDT"), result);
    }

    @Test
    void getHourKlines_shouldChunkRequestsAggregate() {
        // given
        Instant start = Instant.parse("2025-01-01T00:00:00Z");
        Instant end   = start.plus(Duration.ofHours(2000)); // ensures 2 chunks
        CorrelationRequest request = new CorrelationRequest(
                List.of("ALCHUSDT", "FHEUSDT"),
                start,
                end,
                "USDT"
        );

        Kline k1 = Kline.builder()
                .startTime(Instant.parse("2025-01-01T00:00:00Z").toEpochMilli())
                .build();

        Kline k2 = Kline.builder()
                .startTime(Instant.parse("2025-01-20T00:00:00Z").toEpochMilli())
                .build();

        Kline k3 = Kline.builder()
                .startTime(Instant.parse("2025-02-10T00:00:00Z").toEpochMilli())
                .build();

        Kline k4 = Kline.builder()
                .startTime(Instant.parse("2025-02-20T00:00:00Z").toEpochMilli())
                .build();

        KlineResult result1 = new KlineResult("spot", "BTCUSDT", List.of(k2, k1));
        KlineResult result2 = new KlineResult("spot", "BTCUSDT", List.of(k4, k3));

        ExchangeResponse<KlineResult> resp1 =
                new ExchangeResponse<>(0, "OK", result1, "1000");

        ExchangeResponse<KlineResult> resp2 =
                new ExchangeResponse<>(0, "OK", result2, "2000");

        // Mock calls
        when(publicApiClient.getCandlesHistoryByDate(
                eq("spot"),
                eq("BTCUSDT"),
                eq("60"),
                eq("1000"),
                anyString(),
                anyString()
        ))
                .thenReturn(resp1)   // chunk #1
                .thenReturn(resp2);  // chunk #2

        // when
        List<Kline> output = byBitExchangeAdapter.getHourKlines("BTCUSDT", "60", request);

        // then
        assertEquals(4, output.size());
        assertEquals(List.of(k1, k2, k3, k4), output);

        verify(publicApiClient, times(2)).getCandlesHistoryByDate(
                eq("spot"),
                eq("BTCUSDT"),
                eq("60"),
                eq("1000"),
                anyString(),
                anyString()
        );
    }

}