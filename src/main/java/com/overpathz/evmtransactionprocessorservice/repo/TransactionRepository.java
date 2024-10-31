package com.overpathz.evmtransactionprocessorservice.repo;

import com.overpathz.evmtransactionprocessorservice.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

    @Query("SELECT t FROM TransactionEntity t WHERE " +
           "(:fromAddress IS NULL OR t.fromAddress = :fromAddress) AND " +
           "(:toAddress IS NULL OR t.toAddress = :toAddress)")
    List<TransactionEntity> searchTransactions(@Param("fromAddress") String fromAddress,
                                               @Param("toAddress") String toAddress);
}
