package com.company.material.repository;

import com.company.material.entity.PurchaseContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseContractRepository extends JpaRepository<PurchaseContract, Long> {
    Page<PurchaseContract> findBySupplierId(Long supplierId, Pageable pageable);
    List<PurchaseContract> findBySupplierId(Long supplierId);
    Page<PurchaseContract> findByContractStatus(String contractStatus, Pageable pageable);
    List<PurchaseContract> findByExpiryDateBetween(LocalDate start, LocalDate end);
    List<PurchaseContract> findBySupplierIdAndEffectiveDateBeforeAndExpiryDateAfter(
            Long supplierId, LocalDate date1, LocalDate date2);
    boolean existsByContractNo(String contractNo);
}
