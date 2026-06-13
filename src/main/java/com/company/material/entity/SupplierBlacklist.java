package com.company.material.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "supplier_blacklist")
public class SupplierBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long supplierId;

    @Column(nullable = false, length = 500)
    private String blacklistReason;

    private LocalDateTime blacklistDate;

    private Long operatorId;

    @Column(length = 50)
    private String operatorName;

    @Column(length = 20)
    private String status;

    @Column(length = 500)
    private String unblacklistReason;

    private Long unblacklistApproverId;

    @Column(length = 50)
    private String unblacklistApproverName;

    private LocalDateTime unblacklistDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.blacklistDate = LocalDateTime.now();
        if (this.status == null) this.status = "生效中";
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
