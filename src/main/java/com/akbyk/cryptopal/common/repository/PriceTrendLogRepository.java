package com.akbyk.cryptopal.common.repository;

import com.akbyk.cryptopal.common.entity.PriceTrendLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTrendLogRepository extends JpaRepository<PriceTrendLogEntity, Long> {
}