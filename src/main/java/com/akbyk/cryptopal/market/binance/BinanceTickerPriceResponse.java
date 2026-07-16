package com.akbyk.cryptopal.market.binance;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BinanceTickerPriceResponse {
    private String symbol;
    private String price;
}