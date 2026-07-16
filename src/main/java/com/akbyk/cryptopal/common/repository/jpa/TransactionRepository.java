package com.akbyk.cryptopal.common.repository.jpa;

import com.akbyk.cryptopal.common.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
}