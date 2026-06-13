package com.company.material.repository;

import com.company.material.entity.SupplierQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SupplierQualificationRepository extends JpaRepository<SupplierQualification, Long> {
    List<SupplierQualification> findBySupplierId(Long supplierId);
    List<SupplierQualification> findByValidToBetween(LocalDate start, LocalDate end);
    List<SupplierQualification> findBySupplierIdAndValidToAfter(Long supplierId, LocalDate date);
}
