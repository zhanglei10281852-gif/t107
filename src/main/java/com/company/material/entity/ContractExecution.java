package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contract_executions")
public class ContractExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contractId;

    @Column(length = 50)
    private String orderNo;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal executionAmount;

    @Column(nullable = false)
    private LocalDate executionDate;

    @Column(length = 200)
    private String description;

    private Long operatorId;

    @Column(length = 50)
    private String operatorName;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
