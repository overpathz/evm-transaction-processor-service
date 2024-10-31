package com.overpathz.evmtransactionprocessorservice.service;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Async;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvmService {

    @Value("${WEB3J_CLIENT_ADDRESS}")
    private String clientUrl;

    private final TransactionRepository transactionRepository;

    private Web3j web3j;

    private BigInteger lastProcessedBlock;

    private static final int BATCH_SIZE = 100; // entities to save in batch way

    private ExecutorService executorService;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(clientUrl), 2000, Async.defaultExecutorService());
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(availableProcessors);
        resumeProcessing();
    }

    private void resumeProcessing() {
        lastProcessedBlock = transactionRepository.findMaxBlockNumber();
        if (lastProcessedBlock == null) {
            lastProcessedBlock = BigInteger.ZERO;
        }

        // we start processing from last processed block
        startBlockListener(lastProcessedBlock.add(BigInteger.ONE));
    }

    private void startBlockListener(BigInteger startBlock) {
        web3j.replayPastAndFutureBlocksFlowable(
                        DefaultBlockParameter.valueOf(startBlock), true)
                .subscribe(ethBlock -> {
                    executorService.submit(() -> processBlock(ethBlock.getBlock()));
                }, error -> log.error("Error in block subscription", error));
    }

    private void processBlock(EthBlock.Block block) {
        BigInteger blockNumber = block.getNumber();
        log.info("Processing block {}", blockNumber);

        List<EthBlock.TransactionResult> transactions = block.getTransactions();
        List<TransactionEntity> transactionEntities = new ArrayList<>();

        for (EthBlock.TransactionResult txResult : transactions) {
            Transaction tx = (Transaction) txResult.get();
            TransactionEntity transactionEntity = mapToEntity(tx);
            transactionEntities.add(transactionEntity);

            if (transactionEntities.size() >= BATCH_SIZE) {
                saveTransactions(new ArrayList<>(transactionEntities));
                transactionEntities.clear();
            }
        }

        if (!transactionEntities.isEmpty()) {
            saveTransactions(transactionEntities);
        }

        lastProcessedBlock = blockNumber;
    }

    private void saveTransactions(List<TransactionEntity> transactions) {
        try {
            transactionRepository.saveAll(transactions);
            log.info("Saved {} transactions", transactions.size());
        } catch (Exception e) {
            log.error("Error saving transactions", e);
            // do we need to do a retries or maybe some DLQ mechanism?
        }
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
        return entity;
    }
}
