package com.company.material.scheduler;

import com.company.material.entity.Supplier;
import com.company.material.entity.SupplierEvaluation;
import com.company.material.repository.SupplierEvaluationRepository;
import com.company.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SupplierStatusScheduler {

    private final SupplierRepository supplierRepository;
    private final SupplierEvaluationRepository evaluationRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkConsecutiveDFailedSuppliers() {
        List<Supplier> qualified = supplierRepository.findByApprovalStatus("合格供应商");
        for (Supplier s : qualified) {
            List<SupplierEvaluation> recent = evaluationRepository
                    .findBySupplierIdOrderByEvaluatedAtDesc(s.getId());
            if (recent != null && recent.size() >= 2) {
                boolean twoD = "D".equals(recent.get(0).getGrade())
                        && "D".equals(recent.get(1).getGrade());
                if (twoD) {
                    s.setApprovalStatus("暂停合作");
                    s.setStatus("暂停合作");
                    supplierRepository.save(s);
                }
            }
        }
    }
}
