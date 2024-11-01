package com.overpathz.evmtransactionprocessorservice.repo;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    @Query("SELECT t FROM TransactionEntity t WHERE " +
            "(:fromAddress IS NULL OR t.fromAddress = :fromAddress) AND " +
            "(:toAddress IS NULL OR t.toAddress = :toAddress) AND " +
            "(:blockNumber IS NULL OR t.blockNumber = :blockNumber)")
    Page<TransactionEntity> searchTransactions(@Param("fromAddress") String fromAddress,
                                               @Param("toAddress") String toAddress,
                                               @Param("blockNumber") BigInteger blockNumber,
                                               Pageable pageable);

    @Query("SELECT MAX(e.blockNumber) FROM TransactionEntity e")
    Optional<BigInteger> findMaxBlockNumber();

    @Query(value = "SELECT * FROM transactions t WHERE t.search_vector @@ plainto_tsquery(:query)",
            countQuery = "SELECT count(*) FROM transactions t WHERE t.search_vector @@ plainto_tsquery(:query)",
            nativeQuery = true)
    Page<TransactionEntity> searchFullText(@Param("query") String query, Pageable pageable);

    long count();

    long countByTimestampAfter(Timestamp timestamp);

    @Query("SELECT COUNT(DISTINCT t.blockNumber) FROM TransactionEntity t")
    long countDistinctByBlockNumber();

    @Query("SELECT COALESCE(SUM(t.value), 0) FROM TransactionEntity t")
    BigInteger sumTransactionValue();

    @Query("SELECT COALESCE(SUM(t.gas), 0) FROM TransactionEntity t")
    BigInteger sumGas();

    @Query("SELECT COALESCE(AVG(t.gasPrice), 0) FROM TransactionEntity t")
    BigInteger calculateAverageGasPrice();
}
