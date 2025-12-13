package com.spayker.crypto.analysis.service.dto;

import com.spayker.crypto.analysis.dao.feign.bybit.dto.kline.Kline;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistory {

    private List<Kline> klines;

}