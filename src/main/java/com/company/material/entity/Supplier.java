package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String supplierCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String contactPerson;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 50)
    private String businessLicenseNo;

    @Column(length = 50)
    private String taxNo;

    @Column(length = 100)
    private String bankName;

    @Column(length = 50)
    private String bankAccount;

    @Column(precision = 15, scale = 2)
    private BigDecimal registeredCapital;

    @Column(length = 500)
    private String businessScope;

    @Column(length = 20)
    private String approvalStatus;

    @Column(length = 200)
    private String approvalRemark;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "合作中";
        if (this.approvalStatus == null) this.approvalStatus = "待审核";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
