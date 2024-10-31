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

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvmService {

    @Value("${WEB3J_CLIENT_ADDRESS}")
    private String clientUrl;

    private final TransactionRepository transactionRepository;

    private Web3j web3j;

    private BigInteger lastProcessedBlock;

    @PostConstruct
    public void init() {
        web3j = Web3j.build(new HttpService(clientUrl));
        resumeProcessing();
    }

    private void resumeProcessing() {
        lastProcessedBlock = transactionRepository.findMaxBlockNumber();
        if (lastProcessedBlock == null) {
            lastProcessedBlock = BigInteger.ZERO;
        }

        startBlockListener(lastProcessedBlock.add(BigInteger.ONE));
    }

    private void startBlockListener(BigInteger startBlock) {
        web3j.replayPastAndFutureBlocksFlowable(
                        DefaultBlockParameter.valueOf(startBlock), true)
                .subscribe(ethBlock -> processBlock(ethBlock.getBlock()), error -> log.error("Error in block subscription", error));
    }

    private void processBlock(EthBlock.Block block) {
        BigInteger blockNumber = block.getNumber();
        log.info("Processing block {}", blockNumber);

        List<EthBlock.TransactionResult> transactions = block.getTransactions();
        for (EthBlock.TransactionResult txResult : transactions) {
            Transaction tx = (Transaction) txResult.get();
            try {
                TransactionEntity transactionEntity = mapToEntity(tx);
                transactionRepository.save(transactionEntity);
                log.info("Saved transaction: {}", tx.getHash());
            } catch (Exception e) {
                log.error("Error saving transaction: {}", tx.getHash(), e);
            }
        }

        lastProcessedBlock = blockNumber;
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
