package com.spayker.crypto.analysis.service;

import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import com.spayker.crypto.analysis.service.data.ByBitExchangeAdapter;
import com.spayker.crypto.analysis.service.data.CandleHistoryManager;
import com.spayker.crypto.analysis.service.data.TradeDataHistoryStorage;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationRequest;
import com.spayker.crypto.analysis.service.dto.TradeHistory;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationPair;
import com.spayker.crypto.analysis.service.dto.correlation.CorrelationResponse;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.spayker.crypto.analysis.service.validator.BusinessRuleValidator.validateRequest;
import static com.spayker.crypto.analysis.service.validator.BusinessRuleValidator.validateResolvedPairs;

@Slf4j
@Service
@AllArgsConstructor
public class CorrelationManager {

    private final ByBitExchangeAdapter byBitExchangeAdapter;
    private final TradeDataHistoryStorage tradeDataHistoryStorage;
    private final CandleHistoryManager candleHistoryManager;

    public CorrelationResponse getCorrelation(String targetCoin, CorrelationRequest correlationRequestDto) {
        validateRequest(targetCoin, correlationRequestDto);
        List<String> pairNames = resolvePairs(correlationRequestDto);
        validateResolvedPairs(correlationRequestDto, pairNames);
        candleHistoryManager.initCandleStickHistoryByPairName(
                targetCoin,
                pairNames,
                correlationRequestDto
        );
        return calculatePercentageCorrelations(
                pairNames,
                targetCoin,
                correlationRequestDto.getStableCoin()
        );
    }

    private List<String> resolvePairs(CorrelationRequest request) {
        return request.getSymbols() == null
                ? byBitExchangeAdapter.getAllPairNames()
                    .stream()
                    .filter(p -> p.endsWith(request.getStableCoin()))
                    .toList()
                : request.getSymbols();
    }

    private CorrelationResponse calculatePercentageCorrelations(List<String> pairNames, String targetCoin, String stableCoin) {
        List<CorrelationPair> percentageList = new ArrayList<>();
        TradeHistory coinHistory = tradeDataHistoryStorage.getCandleStickHistory().get(targetCoin + stableCoin);
        List<Kline> coinCandleSticks = coinHistory.getKlines();
        pairNames.stream()
                .filter(pN -> !pN.equalsIgnoreCase(targetCoin))
                .forEach(pN -> addPercentageItem(pN, stableCoin, percentageList));
        return CorrelationResponse.builder()
                .targetCoinPercentChange(String.valueOf(getPricePercentage(coinCandleSticks)))
                .correlationPairs(percentageList.stream().sorted(Comparator.comparingDouble(c -> Double.parseDouble(c.getPercentage())))
                .toList()).build();
    }

    private void addPercentageItem(String pairName, String stableCoin, List<CorrelationPair> percentageList) {
        Map<String, TradeHistory> candleStickHistory = tradeDataHistoryStorage.getCandleStickHistory();
        if (candleStickHistory.containsKey(pairName)) {
            double pricePercentage = getPricePercentage(candleStickHistory.get(pairName).getKlines());
            if (pricePercentage != 0) {
                percentageList.add(new CorrelationPair(pairName.replace(stableCoin, ""), String.valueOf(pricePercentage)));
            }
        }
    }

    private double getPricePercentage(List<Kline> candleSticks) {
        double minPrice = candleSticks.stream()
                .mapToDouble(Kline::getLowPrice)
                .min()
                .orElse(0);
        double maxPrice = candleSticks.stream()
                .mapToDouble(Kline::getHighPrice)
                .max()
                .orElse(0);
        double percentage = ((maxPrice - minPrice) / minPrice) * 100.0;
        double lastClose = candleSticks.getLast().getClosePrice();
        double firstOpen = candleSticks.getFirst().getOpenPrice();
        if (firstOpen > lastClose) {
            percentage = -Math.abs(percentage);
        }
        return percentage;
    }
}