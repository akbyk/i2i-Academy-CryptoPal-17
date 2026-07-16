package com.akbyk.cryptopal.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_trend_log")
@Getter
@Setter
@NoArgsConstructor
public class PriceTrendLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_symbol", nullable = false, length = 16)
    private String assetSymbol;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal price;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;
}