package com.akbyk.cryptopal.common.repository.jpa;

import com.akbyk.cryptopal.common.entity.PriceTrendLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PriceTrendLogRepository extends JpaRepository<PriceTrendLogEntity, Long> {
    @Query("select p from PriceTrendLogEntity p where p.assetSymbol = :symbol order by p.recordedAt desc")
    List<PriceTrendLogEntity> findRecentBySymbol(@Param("symbol") String symbol, Pageable pageable);
}