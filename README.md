# crypto-analysis-coin-correlation

[![Build Status](https://github.com/Spayker/crypto-analysis-coin-correlation/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Spayker/crypto-analysis-coin-correlation/actions/workflows/ci.yml) &nbsp;
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://github.com/Spayker/crypto-analysis-coin-correlation/blob/main/LICENSE)



Crypto trade coin correlation service provides price change percentage between target coin and other, available on spot market of ByBit.
Typical request:
```GET: http://localhost:8085/v1/BTC/correlations?```
```
    symbols=ALCHUSDT&symbols=FHEUSDT
    &startDateTime=2025-12-01T00:00:00Z
    &endDateTime=2025-12-10T00:00:00Z
    &stableCoin=USDT
```

More information about the API can be found in Medium.

## Architecture
![alt text](resources/jpg/Price_Correlation.jpg)

## Technical Stack
1) Java 21
2) SpringBoot 3
3) Feign client
4) resilience4j
5) jackson
6) gson
7) JUnit5
8) GitHub actions

## How To Run
Use next VM options: ```-Dspring.profiles.active=bybit```<br>


## License
GNU 3