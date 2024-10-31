package com.overpathz.evmtransactionprocessorservice.service;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.http.HttpService;

import java.sql.Timestamp;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvmService {

    @Value("${WEB3J_CLIENT_ADDRESS}")
    private String clientUrl;

    private final TransactionRepository transactionRepository;

    private Web3j web3j;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(clientUrl));
        startListening();
    }

    public void startListening() {
        web3j.transactionFlowable().subscribe(tx -> {
            try {
                log.info("Found transaction. #Hash={}", tx.getHash());
                TransactionEntity transactionEntity = mapToEntity(tx);
                transactionRepository.save(transactionEntity);
                log.info("Saved transaction: {}", tx.getHash());
            } catch (Exception e) {
                log.error("Error saving transaction: {}", tx.getHash(), e);
            }
        }, error -> log.error("Error in transaction subscription", error));
    }

    private TransactionEntity mapToEntity(Transaction tx) {
        TransactionEntity entity = new TransactionEntity();
        entity.setHash(tx.getHash());
        entity.setFromAddress(tx.getFrom());
        entity.setToAddress(tx.getTo());
        entity.setValue(tx.getValue());
        entity.setGas(tx.getGas());
        entity.setGasPrice(tx.getGasPrice());
        entity.setBlockNumber(tx.getBlockNumber());
        entity.setTimestamp(new Timestamp(System.currentTimeMillis()));
        entity.setPartitionDate(LocalDate.now());
        // Set the search_vector if using full-text search
        return entity;
    }
}
