package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "supplier_evaluations")
public class SupplierEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false, length = 20)
    private String evaluationPeriod;

    @Column(nullable = false, length = 10)
    private String evaluationType;

    private Integer qualityQualifiedBatches;

    private Integer qualityTotalBatches;

    private Integer deliveryOnTimeBatches;

    private Integer deliveryTotalBatches;

    @Column(precision = 5, scale = 2)
    private BigDecimal qualityScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal deliveryScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal priceScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal serviceScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(length = 2)
    private String grade;

    @Column(length = 500)
    private String evaluationRemark;

    private Long evaluatorId;

    @Column(length = 50)
    private String evaluatorName;

    private LocalDateTime evaluatedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
