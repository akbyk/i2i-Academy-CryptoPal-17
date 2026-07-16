package com.akbyk.cryptopal.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "wallet_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "asset_symbol"})
)
@Getter
@Setter
@NoArgsConstructor
public class WalletBalanceEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "asset_symbol", nullable = false, length = 16)
    private String assetSymbol;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal amount = BigDecimal.ZERO;

    // Optimistic locking: secondary safeguard against concurrent trade race
    // conditions, in addition to @Transactional isolation added on Day 3.
    @Version
    @Column(nullable = false)
    private Long version;
}