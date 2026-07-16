package com.akbyk.cryptopal.common.repository.jpa;

import com.akbyk.cryptopal.common.entity.TransactionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByUserIdOrderByExecutedAtDesc(UUID userId, Pageable pageable);
}