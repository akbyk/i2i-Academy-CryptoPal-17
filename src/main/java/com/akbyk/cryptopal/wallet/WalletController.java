package com.akbyk.cryptopal.wallet;

import com.akbyk.cryptopal.auth.AuthenticatedUserContext;
import com.akbyk.cryptopal.wallet.dto.PortfolioResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@Tag(name = "Wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/portfolio")
    @Operation(summary = "Get the authenticated user's current cash balance, holdings, and total net worth")
    public ResponseEntity<PortfolioResponseDto> getPortfolio() {
        UUID userId = AuthenticatedUserContext.getCurrentUserId();
        return ResponseEntity.ok(walletService.getPortfolio(userId));
    }
}
