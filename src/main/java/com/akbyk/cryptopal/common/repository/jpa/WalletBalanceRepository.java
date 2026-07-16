package com.akbyk.cryptopal.common.repository.jpa;

import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository extends JpaRepository<WalletBalanceEntity, UUID> {
    Optional<WalletBalanceEntity> findByUserIdAndAssetSymbol(UUID userId, String assetSymbol);

    List<WalletBalanceEntity> findAllByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletBalanceEntity w where w.userId = :userId and w.assetSymbol = :assetSymbol")
    Optional<WalletBalanceEntity> findByUserIdAndAssetSymbolForUpdate(
            @Param("userId") UUID userId, @Param("assetSymbol") String assetSymbol);

}