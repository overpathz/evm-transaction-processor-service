package com.overpathz.evmtransactionprocessorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.sql.Timestamp;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_from_address", columnList = "from_address"),
                @Index(name = "idx_to_address", columnList = "to_address"),
                @Index(name = "idx_block_number", columnList = "block_number")
        },
        uniqueConstraints = @UniqueConstraint(columnNames = {"hash", "block_number"})
)
@NoArgsConstructor
@Getter
@Setter
public class TransactionEntity {
    @Id
    private String hash;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "to_address")
    private String toAddress;

    @Column(precision = 38, scale = 0)
    private BigInteger value;

    @Column(precision = 38, scale = 0)
    private BigInteger gas;

    @Column(name = "gas_price", precision = 38, scale = 0)
    private BigInteger gasPrice;

    @Column(name = "block_number", nullable = false)
    private BigInteger blockNumber;

    @Column(nullable = false)
    private Timestamp timestamp;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;
}
