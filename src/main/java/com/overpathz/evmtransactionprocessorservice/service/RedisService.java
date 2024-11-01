package com.overpathz.evmtransactionprocessorservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String LAST_PROCESSED_BLOCK_KEY = "last-processed-block";

    public void saveLastProcessedBlock(BigInteger blockNumber) {
        redisTemplate.opsForValue().set(LAST_PROCESSED_BLOCK_KEY, blockNumber.toString());
    }

    public BigInteger getLastProcessedBlock() {
        String value = redisTemplate.opsForValue().get(LAST_PROCESSED_BLOCK_KEY);
        return value != null ? new BigInteger(value) : null;
    }
}
