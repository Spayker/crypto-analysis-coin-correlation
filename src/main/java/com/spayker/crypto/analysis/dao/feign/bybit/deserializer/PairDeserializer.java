package com.spayker.crypto.analysis.dao.feign.bybit.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.spayker.crypto.analysis.dao.feign.bybit.dto.pair.Pair;

import java.io.IOException;


public class PairDeserializer extends JsonDeserializer<Pair> {

    @Override
    public Pair deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        ObjectCodec oc = jsonParser.getCodec();
        JsonNode node = oc.readTree(jsonParser);
        return Pair.builder()
                .symbol(node.get("symbol").asText())
                .baseCoin(node.get("baseCoin").asText())
                .quoteCoin(node.get("quoteCoin").asText())
                .innovation(node.get("innovation").asText())
                .status(node.get("status").asText())
                .marginTrading(node.get("marginTrading").asText())
                .stTag(node.get("stTag").asText())
                .build();
    }



}
