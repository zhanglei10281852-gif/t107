package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "purchase_contracts")
public class PurchaseContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String contractNo;

    @Column(nullable = false, length = 200)
    private String contractName;

    @Column(nullable = false)
    private Long supplierId;

    @Column(precision = 15, scale = 2)
    private BigDecimal contractAmount;

    private LocalDate signDate;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(length = 500)
    private String paymentTerms;

    @Column(nullable = false, length = 20)
    private String contractType;

    @Column(nullable = false, length = 20)
    private String contractStatus;

    @Column(precision = 15, scale = 2)
    private BigDecimal executedAmount;

    @Column(length = 500)
    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.contractStatus == null) this.contractStatus = "草稿";
        if (this.executedAmount == null) this.executedAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
