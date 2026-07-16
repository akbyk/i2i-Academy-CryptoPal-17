package com.akbyk.cryptopal.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class TransactionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "asset_symbol", nullable = false, length = 16)
    private String assetSymbol;

    // Kept as String to mirror the DB CHECK constraint ('BUY'/'SELL') directly;
    // an enum + @Enumerated(STRING) is an equally valid alternative.
    @Column(name = "transaction_type", nullable = false, length = 8)
    private String transactionType;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal volume;

    @Column(name = "execution_price", nullable = false, precision = 24, scale = 8)
    private BigDecimal executionPrice;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private OffsetDateTime executedAt;
}