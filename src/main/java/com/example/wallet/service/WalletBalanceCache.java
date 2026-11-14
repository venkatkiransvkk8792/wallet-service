package com.example.wallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

@Service
public class WalletBalanceCache {
    private static final Logger log = LoggerFactory.getLogger(WalletBalanceCache.class);
    private static final Duration TTL = Duration.ofSeconds(60);
    private final StringRedisTemplate redis;
    private final boolean enabled;

    public WalletBalanceCache(StringRedisTemplate redis,
                              org.springframework.core.env.Environment environment) {
        this.redis = redis;
        this.enabled = Boolean.parseBoolean(
                environment.getProperty("wallet.redis.enabled", "true"));
    }

    private String key(String walletId) {
        return "wallet:balance:" + walletId;
    }

    public Optional<BigDecimal> get(String walletId) {
        if (!enabled) return Optional.empty();
        try {
            String value = redis.opsForValue().get(key(walletId));
            return value == null ? Optional.empty() : Optional.of(new BigDecimal(value));
        } catch (Exception e) {
            log.warn("Redis unavailable while reading wallet balance; falling back to PostgreSQL");
            return Optional.empty();
        }
    }

    public void put(String walletId, BigDecimal balance) {
        if (!enabled) return;
        try {
            redis.opsForValue().set(key(walletId), balance.toPlainString(), TTL);
        } catch (Exception e) {
            log.warn("Redis unavailable while caching wallet balance");
        }
    }

    public void evict(String... walletIds) {
        if (!enabled) return;
        try {
            for (String walletId : walletIds) {
                redis.delete(key(walletId));
            }
        } catch (Exception e) {
            log.warn("Redis unavailable while invalidating wallet balance cache");
        }
    }
}
