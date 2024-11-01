package com.overpathz.evmtransactionprocessorservice.service;

import com.overpathz.evmtransactionprocessorservice.dto.ProcessedTransactionInfoDto;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.sql.Timestamp;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;

    // methods below can use @Cacheable, but it's tricky due some moments
    public ProcessedTransactionInfoDto getTransactionInfo() {
        return new ProcessedTransactionInfoDto(
                getTotalTransactionCount(),
                getTransactionsCountLast5Minutes(),
                getUniqueBlocksProcessed(),
                getTotalValueTransferred(),
                getTotalGasUsed(),
                getAverageGasPrice()
        );
    }

    public long getTotalTransactionCount() {
        return transactionRepository.count();
    }

    private long getTransactionsCountLast5Minutes() {
        Timestamp fiveMinutesAgo = new Timestamp(System.currentTimeMillis() - 5 * 60 * 1000);
        return transactionRepository.countByTimestampAfter(fiveMinutesAgo);
    }

    private long getUniqueBlocksProcessed() {
        return transactionRepository.countDistinctByBlockNumber();
    }

    private BigInteger getTotalValueTransferred() {
        return transactionRepository.sumTransactionValue();
    }

    private BigInteger getTotalGasUsed() {
        return transactionRepository.sumGas();
    }

    private BigInteger getAverageGasPrice() {
        return transactionRepository.calculateAverageGasPrice();
    }
}

