package com.company.material.repository;

import com.company.material.entity.ContractExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContractExecutionRepository extends JpaRepository<ContractExecution, Long> {
    List<ContractExecution> findByContractIdOrderByExecutionDateDesc(Long contractId);
}
