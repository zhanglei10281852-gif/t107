package com.company.material.repository;

import com.company.material.entity.SupplierBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupplierBlacklistRepository extends JpaRepository<SupplierBlacklist, Long> {
    Optional<SupplierBlacklist> findBySupplierIdAndStatus(Long supplierId, String status);
    Page<SupplierBlacklist> findByStatus(String status, Pageable pageable);
    boolean existsBySupplierIdAndStatus(Long supplierId, String status);
}
