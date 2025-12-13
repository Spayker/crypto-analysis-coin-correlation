package com.spayker.crypto.analysis.service.data;

import com.spayker.crypto.analysis.service.dto.TradeHistory;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TradeDataHistoryStorage {

    @Getter
    private final Map<String, TradeHistory> candleStickHistory = new ConcurrentHashMap<>();

}
