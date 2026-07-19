package com.akbyk.cryptopal.common.exception;

import com.akbyk.cryptopal.ai.AiRateLimitExceededException;
import com.akbyk.cryptopal.auth.DuplicateUserException;
import com.akbyk.cryptopal.auth.InvalidCredentialsException;
import com.akbyk.cryptopal.common.dto.ErrorResponseDto;
import com.akbyk.cryptopal.trading.AssetNotFoundException;
import com.akbyk.cryptopal.trading.InsufficientFundsException;
import com.akbyk.cryptopal.trading.InvalidAmountException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateUser(DuplicateUserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDto("DUPLICATE_USER", ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDto("INVALID_CREDENTIALS", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDto("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponseDto("INSUFFICIENT_FUNDS", ex.getMessage()));
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidAmount(InvalidAmountException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDto("INVALID_AMOUNT", ex.getMessage()));
    }

    @ExceptionHandler(AssetNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleAssetNotFound(AssetNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto("ASSET_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AiRateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleAiRateLimitExceeded(AiRateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponseDto("AI_RATE_LIMIT_EXCEEDED", ex.getMessage()));
    }
}