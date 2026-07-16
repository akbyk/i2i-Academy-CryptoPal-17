package com.akbyk.cryptopal.trading.dto;


import com.akbyk.cryptopal.trading.TradeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TradeRequestDto {

    @NotBlank
    private String assetSymbol;

    @NotNull
    private TradeType type;

    @NotNull
    @DecimalMin(value = "0.00000001", message = "Volume must be greater than zero")
    private BigDecimal volume;
}