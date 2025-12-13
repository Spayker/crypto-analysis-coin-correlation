package com.spayker.crypto.analysis.dao.feign.bybit;


import com.spayker.crypto.analysis.dao.feign.bybit.dto.ExchangeResponse;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.KlineResult;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.PairListResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Profile("bybit")
@FeignClient(name = "${exchange.rest-provider.public-api-name}", url = "${exchange.rest-provider.url}")
public interface PublicApiClient {

    String LIMIT = "limit";
    String INTERVAL = "interval";
    String SYMBOL = "symbol";
    String CATEGORY = "category";
    String START = "start";
    String END = "end";

    @GetMapping(value = "/v5/market/instruments-info")
    ExchangeResponse<PairListResult> getPairNames(@RequestParam(CATEGORY) final String category);

    @GetMapping(value = "/v5/market/kline")
    ExchangeResponse<KlineResult> getCandlesHistory(@RequestParam(CATEGORY) final String category,
                                                    @RequestParam(SYMBOL) final String symbol,
                                                    @RequestParam(INTERVAL) final String interval,
                                                    @RequestParam(LIMIT) final String limit);

    @GetMapping(value = "/v5/market/kline")
    ExchangeResponse<KlineResult> getCandlesHistoryByDate(@RequestParam(CATEGORY) final String category,
                                                    @RequestParam(SYMBOL) final String symbol,
                                                    @RequestParam(INTERVAL) final String interval,
                                                    @RequestParam(LIMIT) final String limit,
                                                    @RequestParam(START) final String start,
                                                    @RequestParam(END) final String end);

}