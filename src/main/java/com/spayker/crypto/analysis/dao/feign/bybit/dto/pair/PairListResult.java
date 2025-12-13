package com.spayker.crypto.analysis.dao.feign.bybit.dto.pair;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.ExchangeResponseResult;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class PairListResult implements ExchangeResponseResult {

    private String category;

    @JsonProperty("list")
    @SerializedName(value = "list", alternate = "pairs")
    private List<Pair> pairs;

}
