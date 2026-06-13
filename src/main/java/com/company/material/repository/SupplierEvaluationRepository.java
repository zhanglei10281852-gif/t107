package com.company.material.repository;

import com.company.material.entity.SupplierEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupplierEvaluationRepository extends JpaRepository<SupplierEvaluation, Long> {
    List<SupplierEvaluation> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    Page<SupplierEvaluation> findBySupplierId(Long supplierId, Pageable pageable);
    List<SupplierEvaluation> findBySupplierIdOrderByEvaluatedAtDesc(Long supplierId);
    List<SupplierEvaluation> findTop2BySupplierIdAndGradeOrderByEvaluatedAtDesc(Long supplierId, String grade);
}
