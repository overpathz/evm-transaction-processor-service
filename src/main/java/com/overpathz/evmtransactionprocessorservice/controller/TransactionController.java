package com.overpathz.evmtransactionprocessorservice.controller;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/search")
    public ResponseEntity<List<TransactionEntity>> searchTransactions(
            @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String toAddress) {

        List<TransactionEntity> results = transactionRepository.searchTransactions(fromAddress, toAddress);
        return ResponseEntity.ok(results);
    }
}
