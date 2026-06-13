package com.company.material.repository;

import com.company.material.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findBySupplierCode(String supplierCode);
    boolean existsBySupplierCode(String supplierCode);
    Page<Supplier> findByStatus(String status, Pageable pageable);
    Page<Supplier> findByCategory(String category, Pageable pageable);
    Page<Supplier> findByApprovalStatus(String approvalStatus, Pageable pageable);
    List<Supplier> findByApprovalStatus(String approvalStatus);
    long countByApprovalStatus(String approvalStatus);
    long countByStatus(String status);

    @Query("SELECT s.approvalStatus, COUNT(s) FROM Supplier s GROUP BY s.approvalStatus")
    List<Object[]> countByApprovalStatusGroup();

    @Query("SELECT FUNCTION('DATE_FORMAT', s.createdAt, '%Y-%m') as month, " +
           "s.approvalStatus, COUNT(s) FROM Supplier s " +
           "WHERE s.createdAt >= :startDate GROUP BY month, s.approvalStatus ORDER BY month")
    List<Object[]> countQualifiedTrend(LocalDateTime startDate);
}
