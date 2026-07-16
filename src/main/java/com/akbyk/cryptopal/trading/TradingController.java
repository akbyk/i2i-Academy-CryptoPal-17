package com.akbyk.cryptopal.trading;

import com.akbyk.cryptopal.auth.AuthenticatedUserContext;
import com.akbyk.cryptopal.trading.dto.TradeRequestDto;
import com.akbyk.cryptopal.trading.dto.TradeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/trading")
@Tag(name = "Trading")
public class TradingController {

    private final TradingService tradingService;

    public TradingController(TradingService tradingService) {
        this.tradingService = tradingService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Execute a BUY or SELL trade at the current Redis-cached price")
    public ResponseEntity<TradeResponseDto> execute(@Valid @RequestBody TradeRequestDto request) {

        UUID userId = AuthenticatedUserContext.getCurrentUserId();
        return ResponseEntity.ok(tradingService.executeTrade(userId, request));
    }
}