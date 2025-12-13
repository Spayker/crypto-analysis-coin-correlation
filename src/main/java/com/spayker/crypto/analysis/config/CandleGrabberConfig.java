package com.spayker.crypto.analysis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "candle-grabber")
public class CandleGrabberConfig {

    private String minuteIntervalType;
    private String minuteIntervalQuantity;
    private String hourIntervalType;
    private String hourIntervalQuantity;
    private String dayIntervalType;
    private String dayIntervalQuantity;

}