package com.overpathz.evmtransactionprocessorservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@NoArgsConstructor
@Getter
@Setter
public class TransactionEntity {
    @Id
    private String hash;

    @Column(name = "from_address")
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    private BigInteger value;
    private BigInteger gas;
    private BigInteger gasPrice;
    private BigInteger blockNumber;
    private Timestamp timestamp;

    @Column(name = "partition_date")
    private LocalDate partitionDate;
}
