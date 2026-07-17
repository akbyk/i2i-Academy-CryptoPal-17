package com.akbyk.cryptopal.portfolio;

import com.akbyk.cryptopal.auth.AuthenticatedUserContext;
import com.akbyk.cryptopal.common.repository.jpa.TransactionRepository;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import com.akbyk.cryptopal.portfolio.dto.BalanceDto;
import com.akbyk.cryptopal.portfolio.dto.PortfolioResponseDto;
import com.akbyk.cryptopal.portfolio.dto.TransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio")
public class PortfolioController {

    private static final int RECENT_TRANSACTIONS_LIMIT = 50;

    private final WalletBalanceRepository walletBalanceRepository;
    private final TransactionRepository transactionRepository;

    public PortfolioController(WalletBalanceRepository walletBalanceRepository,
                               TransactionRepository transactionRepository) {
        this.walletBalanceRepository = walletBalanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's current balances and recent transaction history")
    public ResponseEntity<PortfolioResponseDto> getPortfolio() {
        UUID userId = AuthenticatedUserContext.getCurrentUserId();

        List<BalanceDto> balances = walletBalanceRepository.findAllByUserId(userId).stream()
                .map(b -> new BalanceDto(b.getAssetSymbol(), b.getAmount()))
                .toList();

        List<TransactionDto> transactions = transactionRepository
                .findByUserIdOrderByExecutedAtDesc(userId, PageRequest.of(0, RECENT_TRANSACTIONS_LIMIT)).stream()
                .map(t -> new TransactionDto(t.getId(), t.getAssetSymbol(), t.getTransactionType(),
                        t.getVolume(), t.getExecutionPrice(), t.getExecutedAt()))
                .toList();

        return ResponseEntity.ok(new PortfolioResponseDto(balances, transactions));
    }
}