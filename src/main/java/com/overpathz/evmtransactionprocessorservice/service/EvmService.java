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
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.Transaction;
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

    private Web3j web3j;

    private BigInteger lastProcessedBlock;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(clientUrl));
        resumeProcessing();
    }

    private void resumeProcessing() {
        lastProcessedBlock = redisService.getLastProcessedBlock();

        if (lastProcessedBlock == null) {
            lastProcessedBlock = getStartingBlockNumber();
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
                        error -> log.error("Error in block subscription", error)
                );
    }

    private void processBlock(EthBlock.Block block) {
        try {
            BigInteger blockNumber = block.getNumber();
            log.info("Processing block {}", blockNumber);

            List<EthBlock.TransactionResult> transactions = block.getTransactions();
            log.info("Found {} transactions in block #{}", transactions.size(), blockNumber);

            Timestamp blockTimestamp = new Timestamp(block.getTimestamp().longValueExact() * 1000);

            processTransactions(transactions, blockTimestamp);

            lastProcessedBlock = blockNumber;
            redisService.saveLastProcessedBlock(lastProcessedBlock);
        } catch (Exception e) {
            log.error("Error processing block {}", block.getNumber(), e);
            // retry if needed ?
        }
    }

    private void processTransactions(List<EthBlock.TransactionResult> transactions, Timestamp blockTimestamp) {
        List<TransactionEntity> transactionEntities = new ArrayList<>();

        for (EthBlock.TransactionResult txResult : transactions) {
            Transaction tx = (Transaction) txResult.get();
            TransactionEntity transactionEntity = mapToEntity(tx, blockTimestamp);
            transactionEntities.add(transactionEntity);

            if (transactionEntities.size() >= batchSize) {
                saveTransactions(new ArrayList<>(transactionEntities));
                transactionEntities.clear();
            }
        }

        if (!transactionEntities.isEmpty()) {
            saveTransactions(transactionEntities);
        }
    }

    private void saveTransactions(List<TransactionEntity> transactions) {
        try {
            transactionRepository.saveAll(transactions);
            log.info("Saved {} transactions", transactions.size());
        } catch (Exception e) {
            log.error("Error saving transactions", e);
            // Implement retry logic or error handling as needed
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
