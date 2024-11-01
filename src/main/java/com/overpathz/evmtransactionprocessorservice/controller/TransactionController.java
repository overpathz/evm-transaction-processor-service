package com.overpathz.evmtransactionprocessorservice.controller;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @Operation(summary = "Search transactions by criteria")
    @GetMapping("/search")
    public Page<TransactionEntity> searchTransactions(
            @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String toAddress,
            @RequestParam(required = false) BigInteger blockNumber,
            Pageable pageable) {
        return transactionRepository.searchTransactions(fromAddress, toAddress, blockNumber, pageable);
    }

    @Operation(summary = "Full-text search in transaction input data")
    @GetMapping("/fulltext")
    public Page<TransactionEntity> fullTextSearch(
            @RequestParam String query,
            Pageable pageable) {
        return transactionRepository.searchFullText(query, pageable);
    }
}
