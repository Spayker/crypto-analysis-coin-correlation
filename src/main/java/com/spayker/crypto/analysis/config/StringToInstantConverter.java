package com.spayker.crypto.analysis.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class StringToInstantConverter implements Converter<String, Instant> {

    @Override
    public Instant convert(String source) {
        return source == null ? null : Instant.parse(source);
    }
}
