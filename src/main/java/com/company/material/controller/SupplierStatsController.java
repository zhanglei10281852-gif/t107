package com.company.material.controller;

import com.company.material.entity.PurchaseContract;
import com.company.material.entity.Supplier;
import com.company.material.entity.SupplierEvaluation;
import com.company.material.entity.SupplierQualification;
import com.company.material.repository.PurchaseContractRepository;
import com.company.material.repository.SupplierEvaluationRepository;
import com.company.material.repository.SupplierQualificationRepository;
import com.company.material.repository.SupplierBlacklistRepository;
import com.company.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supplier-stats")
@RequiredArgsConstructor
public class SupplierStatsController {

    private final SupplierRepository supplierRepository;
    private final SupplierEvaluationRepository evaluationRepository;
    private final PurchaseContractRepository contractRepository;
    private final SupplierQualificationRepository qualificationRepository;
    private final SupplierBlacklistRepository blacklistRepository;

    @GetMapping("/grade-distribution")
    public ResponseEntity<?> gradeDistribution() {
        List<Supplier> suppliers = supplierRepository.findByApprovalStatus("合格供应商");
        Map<String, Integer> gradeCount = new LinkedHashMap<>();
        gradeCount.put("A", 0);
        gradeCount.put("B", 0);
        gradeCount.put("C", 0);
        gradeCount.put("D", 0);
        gradeCount.put("未评估", 0);

        for (Supplier s : suppliers) {
            List<SupplierEvaluation> evals = evaluationRepository.findBySupplierIdOrderByEvaluatedAtDesc(s.getId());
            if (evals.isEmpty()) {
                gradeCount.put("未评估", gradeCount.get("未评估") + 1);
            } else {
                String grade = evals.get(0).getGrade();
                gradeCount.put(grade, gradeCount.getOrDefault(grade, 0) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : gradeCount.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("grade", entry.getKey());
            item.put("count", entry.getValue());
            String desc = switch (entry.getKey()) {
                case "A" -> "优秀(≥90)";
                case "B" -> "良好(≥75)";
                case "C" -> "合格(≥60)";
                case "D" -> "不合格(<60)";
                default -> "暂无评估";
            };
            item.put("description", desc);
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/supplier-ranking")
    public ResponseEntity<?> supplierRanking() {
        List<Supplier> suppliers = supplierRepository.findByApprovalStatus("合格供应商");
        List<Map<String, Object>> ranking = new ArrayList<>();

        for (Supplier s : suppliers) {
            List<SupplierEvaluation> evals = evaluationRepository.findBySupplierIdOrderByEvaluatedAtDesc(s.getId());
            if (!evals.isEmpty()) {
                SupplierEvaluation latest = evals.get(0);
                Map<String, Object> item = new HashMap<>();
                item.put("supplierId", s.getId());
                item.put("supplierCode", s.getSupplierCode());
                item.put("supplierName", s.getName());
                item.put("category", s.getCategory());
                item.put("totalScore", latest.getTotalScore());
                item.put("grade", latest.getGrade());
                item.put("evaluationPeriod", latest.getEvaluationPeriod());
                item.put("qualityScore", latest.getQualityScore());
                item.put("deliveryScore", latest.getDeliveryScore());
                item.put("priceScore", latest.getPriceScore());
                item.put("serviceScore", latest.getServiceScore());
                ranking.add(item);
            }
        }

        ranking.sort((a, b) -> {
            BigDecimal sa = (BigDecimal) a.get("totalScore");
            BigDecimal sb = (BigDecimal) b.get("totalScore");
            return sb.compareTo(sa);
        });

        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }

        return ResponseEntity.ok(ranking);
    }

    @GetMapping("/qualified-trend")
    public ResponseEntity<?> qualifiedTrend(
            @RequestParam(defaultValue = "6") int months) {
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Object[]> rawData = supplierRepository.countQualifiedTrend(startDate);

        Set<String> allMonths = new LinkedHashSet<>();
        LocalDate cursor = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);
        for (int i = 0; i < months; i++) {
            allMonths.add(cursor.getYear() + "-" + String.format("%02d", cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }

        Map<String, Map<String, Long>> monthData = new LinkedHashMap<>();
        for (String m : allMonths) {
            monthData.put(m, new HashMap<>());
        }

        for (Object[] row : rawData) {
            String month = (String) row[0];
            String status = (String) row[1];
            Long count = ((Number) row[2]).longValue();
            if (monthData.containsKey(month)) {
                monthData.get(month).put(status, count);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        long runningTotal = 0;
        for (Map.Entry<String, Map<String, Long>> entry : monthData.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            long qualifiedCount = entry.getValue().getOrDefault("合格供应商", 0L);
            runningTotal += qualifiedCount;
            item.put("qualifiedCount", runningTotal);
            item.put("newPending", entry.getValue().getOrDefault("待审核", 0L));
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/contract-summary")
    public ResponseEntity<?> contractSummary() {
        List<PurchaseContract> contracts = contractRepository.findAll();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalExecuted = BigDecimal.ZERO;
        int performingCount = 0;
        int expiredCount = 0;
        int terminatedCount = 0;
        int draftCount = 0;
        int overExecutedCount = 0;

        Map<String, BigDecimal> typeAmount = new LinkedHashMap<>();
        typeAmount.put("年度框架协议", BigDecimal.ZERO);
        typeAmount.put("单次采购合同", BigDecimal.ZERO);
        typeAmount.put("服务合同", BigDecimal.ZERO);

        for (PurchaseContract c : contracts) {
            if (c.getContractAmount() != null) {
                totalAmount = totalAmount.add(c.getContractAmount());
                typeAmount.merge(c.getContractType(), c.getContractAmount(), BigDecimal::add);
            }
            if (c.getExecutedAmount() != null) {
                totalExecuted = totalExecuted.add(c.getExecutedAmount());
            }
            switch (c.getContractStatus()) {
                case "履行中" -> performingCount++;
                case "已到期" -> expiredCount++;
                case "已终止" -> terminatedCount++;
                case "草稿" -> draftCount++;
            }
            if (c.getContractAmount() != null && c.getExecutedAmount() != null
                    && c.getExecutedAmount().compareTo(c.getContractAmount()) > 0) {
                overExecutedCount++;
            }
        }

        BigDecimal progress = BigDecimal.ZERO;
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            progress = totalExecuted.multiply(BigDecimal.valueOf(100))
                    .divide(totalAmount, 2, java.math.RoundingMode.HALF_UP);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalContractCount", contracts.size());
        result.put("totalAmount", totalAmount);
        result.put("totalExecuted", totalExecuted);
        result.put("executionProgress", progress);
        result.put("performingCount", performingCount);
        result.put("expiredCount", expiredCount);
        result.put("terminatedCount", terminatedCount);
        result.put("draftCount", draftCount);
        result.put("overExecutedCount", overExecutedCount);

        List<Map<String, Object>> typeList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : typeAmount.entrySet()) {
            Map<String, Object> t = new HashMap<>();
            t.put("contractType", e.getKey());
            t.put("amount", e.getValue());
            typeList.add(t);
        }
        result.put("amountByType", typeList);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/expiring-contracts")
    public ResponseEntity<?> expiringContracts(
            @RequestParam(defaultValue = "60") int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        List<PurchaseContract> contracts = contractRepository.findByExpiryDateBetween(today, endDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (PurchaseContract c : contracts) {
            Map<String, Object> item = new HashMap<>();
            item.put("contractId", c.getId());
            item.put("contractNo", c.getContractNo());
            item.put("contractName", c.getContractName());
            item.put("supplierId", c.getSupplierId());
            supplierRepository.findById(c.getSupplierId()).ifPresent(s -> {
                item.put("supplierName", s.getName());
                item.put("supplierCode", s.getSupplierCode());
            });
            item.put("expiryDate", c.getExpiryDate());
            item.put("contractStatus", c.getContractStatus());
            item.put("daysToExpiry", java.time.temporal.ChronoUnit.DAYS.between(today, c.getExpiryDate()));
            item.put("contractAmount", c.getContractAmount());
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) a.get("daysToExpiry"), (Long) b.get("daysToExpiry")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/expiring-qualifications")
    public ResponseEntity<?> expiringQualifications(
            @RequestParam(defaultValue = "30") int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        List<SupplierQualification> quals = qualificationRepository.findByValidToBetween(today, endDate);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplierQualification q : quals) {
            Map<String, Object> item = new HashMap<>();
            item.put("qualificationId", q.getId());
            item.put("certificateName", q.getCertificateName());
            item.put("certificateNo", q.getCertificateNo());
            item.put("validTo", q.getValidTo());
            item.put("supplierId", q.getSupplierId());
            supplierRepository.findById(q.getSupplierId()).ifPresent(s -> {
                item.put("supplierName", s.getName());
                item.put("supplierCode", s.getSupplierCode());
            });
            item.put("daysToExpiry", java.time.temporal.ChronoUnit.DAYS.between(today, q.getValidTo()));
            result.add(item);
        }
        result.sort((a, b) -> Long.compare((Long) a.get("daysToExpiry"), (Long) b.get("daysToExpiry")));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview() {
        Map<String, Object> result = new HashMap<>();

        result.put("totalSuppliers", supplierRepository.count());
        result.put("qualifiedSuppliers", supplierRepository.countByApprovalStatus("合格供应商"));
        result.put("pendingSuppliers", supplierRepository.countByApprovalStatus("待审核"));
        result.put("suspendedSuppliers", supplierRepository.countByApprovalStatus("暂停合作"));
        result.put("blacklistCount", blacklistRepository.count());
        result.put("activeBlacklistCount", blacklistRepository.count() == 0 ? 0 :
                blacklistRepository.findByStatus("生效中", org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements());

        long activeBlacklist = blacklistRepository.findByStatus("生效中",
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        result.put("activeBlacklistCount", activeBlacklist);

        LocalDate today = LocalDate.now();
        List<PurchaseContract> expiringContracts = contractRepository.findByExpiryDateBetween(today, today.plusDays(60));
        List<SupplierQualification> expiringQuals = qualificationRepository.findByValidToBetween(today, today.plusDays(30));

        result.put("expiringContractCount", expiringContracts.size());
        result.put("expiringQualificationCount", expiringQuals.size());

        List<PurchaseContract> allContracts = contractRepository.findAll();
        BigDecimal totalContractAmount = BigDecimal.ZERO;
        BigDecimal totalExecutedAmount = BigDecimal.ZERO;
        for (PurchaseContract c : allContracts) {
            if (c.getContractAmount() != null) totalContractAmount = totalContractAmount.add(c.getContractAmount());
            if (c.getExecutedAmount() != null) totalExecutedAmount = totalExecutedAmount.add(c.getExecutedAmount());
        }
        result.put("totalContractAmount", totalContractAmount);
        result.put("totalExecutedAmount", totalExecutedAmount);
        result.put("totalContractCount", allContracts.size());

        return ResponseEntity.ok(result);
    }
}
