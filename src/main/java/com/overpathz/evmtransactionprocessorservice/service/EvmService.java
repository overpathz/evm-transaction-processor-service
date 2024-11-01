package com.overpathz.evmtransactionprocessorservice.service;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import com.overpathz.evmtransactionprocessorservice.repo.TransactionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import io.reactivex.schedulers.Schedulers;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvmService {

    @Value("${web3j.client-address}")
    private String clientUrl;

    @Value("${transaction.batch-size:100}")
    private int batchSize;

    private final TransactionRepository transactionRepository;
    private final RedisService redisService;
    private final MeterRegistry meterRegistry;

    private Web3j web3j;

    private BigInteger lastProcessedBlock;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(clientUrl));
        resumeProcessing();
    }

    @PreDestroy
    public void shutdown() {
        if (web3j != null) {
            web3j.shutdown();
        }
    }

    private void resumeProcessing() {
        lastProcessedBlock = redisService.getLastProcessedBlock();

        if (lastProcessedBlock == null) {
            // db fallback
            lastProcessedBlock = transactionRepository.findMaxBlockNumber().orElse(null);
            if (lastProcessedBlock != null) {
                log.info("Resuming from last processed block in DB: {}", lastProcessedBlock);
            }
        }

        if (lastProcessedBlock == null) {
            lastProcessedBlock = getStartingBlockNumber();
            log.info("Starting from block: {}", lastProcessedBlock);
        }

        startBlockListener(lastProcessedBlock.add(BigInteger.ONE));
    }

    private BigInteger getStartingBlockNumber() {
        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            return blockNumber.getBlockNumber().subtract(BigInteger.TEN);
        } catch (Exception e) {
            log.error("Failed to fetch starting block number", e);
            return BigInteger.ZERO;
        }
    }

    private void startBlockListener(BigInteger startBlock) {
        web3j.replayPastAndFutureBlocksFlowable(DefaultBlockParameter.valueOf(startBlock), true)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(
                        ethBlock -> processBlock(ethBlock.getBlock()),
                        error -> {
                            log.error("Error in block subscription", error);
                            meterRegistry.counter("evm.block.subscription.errors").increment();
                        }
                );
    }

    private void processBlock(EthBlock.Block block) {
        Timer.Sample blockTimer = Timer.start(meterRegistry);
        try {
            BigInteger blockNumber = block.getNumber();
            log.info("Processing block {}", blockNumber);

            List<EthBlock.TransactionResult> transactions = block.getTransactions();
            int transactionCount = transactions.size();
            log.info("Found {} transactions in block #{}", transactionCount, blockNumber);

            // Record transactions per block
            meterRegistry.counter("evm.transactions.per.block", "blockNumber", blockNumber.toString())
                    .increment(transactionCount);

            Timestamp blockTimestamp = new Timestamp(block.getTimestamp().longValueExact() * 1000);

            processTransactions(transactions, blockTimestamp);

            lastProcessedBlock = blockNumber;
            redisService.saveLastProcessedBlock(lastProcessedBlock);

            blockTimer.stop(meterRegistry.timer("evm.block.processing.time"));

            meterRegistry.counter("evm.blocks.processed.count").increment();

        } catch (Exception e) {
            log.error("Error processing block {}", block.getNumber(), e);
            meterRegistry.counter("evm.block.processing.errors").increment();
        }
    }

    private void processTransactions(List<EthBlock.TransactionResult> transactions, Timestamp blockTimestamp) {
        List<TransactionEntity> transactionEntities = new ArrayList<>();
        int batchesProcessed = 0;

        for (EthBlock.TransactionResult txResult : transactions) {
            Transaction tx = (Transaction) txResult.get();
            TransactionEntity transactionEntity = mapToEntity(tx, blockTimestamp);
            transactionEntities.add(transactionEntity);

            // we have a lot of transaction from flowable, so we can do batch saving to decrease DB load
            if (transactionEntities.size() >= batchSize) {
                saveTransactions(new ArrayList<>(transactionEntities));
                transactionEntities.clear();
                batchesProcessed++;
            }
        }

        if (!transactionEntities.isEmpty()) {
            saveTransactions(transactionEntities);
            batchesProcessed++;
        }

        // Record batches processed
        meterRegistry.counter("evm.transaction.batches.processed").increment(batchesProcessed);
    }

    private void saveTransactions(List<TransactionEntity> transactions) {
        Timer.Sample saveTimer = Timer.start(meterRegistry);
        try {
            transactionRepository.saveAll(transactions);
            log.info("Saved {} transactions", transactions.size());
            meterRegistry.counter("evm.transactions.processed.count").increment(transactions.size());
        } catch (Exception e) {
            log.error("Error saving transactions", e);
            meterRegistry.counter("evm.transaction.saving.errors").increment();
        } finally {
            saveTimer.stop(meterRegistry.timer("evm.transaction.saving.time"));
        }
    }

    private TransactionEntity mapToEntity(Transaction tx, Timestamp blockTimestamp) {
        TransactionEntity entity = new TransactionEntity();
        entity.setHash(tx.getHash());
        entity.setFromAddress(tx.getFrom());
        entity.setToAddress(tx.getTo());
        entity.setValue(tx.getValue());
        entity.setGas(tx.getGas());
        entity.setGasPrice(tx.getGasPrice());
        entity.setBlockNumber(tx.getBlockNumber());
        entity.setTimestamp(blockTimestamp);
        entity.setInputData(tx.getInput());
        return entity;
    }
}
