package com.spayker.crypto.analysis.dao.feign.bybit.dto.pair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.spayker.crypto.analysis.dao.feign.bybit.deserializer.PairDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = PairDeserializer.class)
public class Pair {

    private String symbol;
    private String baseCoin;
    private String quoteCoin;
    private String innovation;
    private String status;
    private String marginTrading;
    private String stTag;

}
