package com.akbyk.cryptopal.common.repository.jpa;

import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository extends JpaRepository<WalletBalanceEntity, UUID> {
    Optional<WalletBalanceEntity> findByUserIdAndAssetSymbol(UUID userId, String assetSymbol);
}