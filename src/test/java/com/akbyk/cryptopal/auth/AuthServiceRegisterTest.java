package com.akbyk.cryptopal.auth;

import com.akbyk.cryptopal.auth.dto.RegisterRequestDto;
import com.akbyk.cryptopal.auth.dto.RegisterResponseDto;
import com.akbyk.cryptopal.common.entity.UserEntity;
import com.akbyk.cryptopal.common.repository.jpa.UserRepository;
import com.akbyk.cryptopal.common.repository.jpa.WalletBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceRegisterTest {

    @Test
    void register_persistsUserAndUsdWalletWithBalanceInSpecRange() {
        UserRepository userRepository = mock(UserRepository.class);
        WalletBalanceRepository walletBalanceRepository = mock(WalletBalanceRepository.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthService authService = new AuthService(
                userRepository, walletBalanceRepository, new BCryptPasswordEncoder(12), redisTemplate);
        ReflectionTestUtils.setField(authService, "sessionTtlSeconds", 3600L);

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("supersecret1");

        RegisterResponseDto response = authService.register(request);

        assertThat(response.startingBalance()).isBetween(
                BigDecimal.valueOf(1000.0), BigDecimal.valueOf(25000.0));
        verify(userRepository, times(1)).save(any(UserEntity.class));
        verify(walletBalanceRepository, times(1)).save(any());
    }

    @Test
    void register_rejectsDuplicateUsernameBeforeTouchingWalletRepository() {
        UserRepository userRepository = mock(UserRepository.class);
        WalletBalanceRepository walletBalanceRepository = mock(WalletBalanceRepository.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

        when(userRepository.existsByUsername("bob")).thenReturn(true);

        AuthService authService = new AuthService(
                userRepository, walletBalanceRepository, new BCryptPasswordEncoder(12), redisTemplate);

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("bob");
        request.setEmail("bob@example.com");
        request.setPassword("supersecret1");

        org.junit.jupiter.api.Assertions.assertThrows(
                DuplicateUserException.class, () -> authService.register(request));

        verifyNoInteractions(walletBalanceRepository);
    }
}
