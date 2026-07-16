package com.akbyk.cryptopal.auth;

import com.akbyk.cryptopal.auth.dto.AuthResponseDto;
import com.akbyk.cryptopal.auth.dto.LoginRequestDto;
import com.akbyk.cryptopal.auth.dto.RegisterRequestDto;
import com.akbyk.cryptopal.auth.dto.RegisterResponseDto;
import com.akbyk.cryptopal.common.entity.UserEntity;
import com.akbyk.cryptopal.common.entity.WalletBalanceEntity;
import com.akbyk.cryptopal.common.repository.UserRepository;
import com.akbyk.cryptopal.common.repository.WalletBalanceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    // Randomized starting balance range for new accounts, per spec §2.1.
    private static final double MIN_STARTING_BALANCE = 1000.0;
    private static final double MAX_STARTING_BALANCE = 25000.0;

    private final UserRepository userRepository;
    private final WalletBalanceRepository walletBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Value("${cryptopal.session.ttl-seconds:3600}")
    private long sessionTtlSeconds;

    public AuthService(UserRepository userRepository,
                       WalletBalanceRepository walletBalanceRepository,
                       PasswordEncoder passwordEncoder,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.walletBalanceRepository = walletBalanceRepository;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already registered: " + request.getEmail());
        }

        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(OffsetDateTime.now());
        user = userRepository.save(user);

        BigDecimal startingBalance = randomStartingBalance();

        WalletBalanceEntity wallet = new WalletBalanceEntity();
        wallet.setUserId(user.getId());
        wallet.setAssetSymbol("USD");
        wallet.setAmount(startingBalance);
        // If this insert throws (constraint violation, connection drop, etc.),
        // @Transactional rolls back the user insert above too — no orphaned
        // users row can exist without its matching USD wallet row.
        walletBalanceRepository.save(wallet);

        return new RegisterResponseDto(user.getId(), user.getUsername(), startingBalance);
    }

    private BigDecimal randomStartingBalance() {
        double raw = ThreadLocalRandom.current().nextDouble(MIN_STARTING_BALANCE, MAX_STARTING_BALANCE);
        return BigDecimal.valueOf(raw).setScale(2, RoundingMode.HALF_UP);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        UserEntity user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        String token = UUID.randomUUID().toString();
        String redisKey = "session:" + token;
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), Duration.ofSeconds(sessionTtlSeconds));

        return new AuthResponseDto(token, sessionTtlSeconds);
    }
}