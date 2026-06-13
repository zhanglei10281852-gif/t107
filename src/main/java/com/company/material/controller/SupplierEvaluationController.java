package com.company.material.controller;

import com.company.material.entity.Supplier;
import com.company.material.entity.SupplierEvaluation;
import com.company.material.repository.SupplierEvaluationRepository;
import com.company.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supplier-evaluations")
@RequiredArgsConstructor
public class SupplierEvaluationController {

    private final SupplierEvaluationRepository evaluationRepository;
    private final SupplierRepository supplierRepository;

    private boolean isPurchasingManagerOrAdmin(String role) {
        return "管理员".equals(role) || "采购主管".equals(role);
    }

    private BigDecimal calculateScore(Integer qualified, Integer total) {
        if (total == null || total == 0 || qualified == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(qualified)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private String calculateGrade(BigDecimal total) {
        if (total.compareTo(BigDecimal.valueOf(90)) >= 0) return "A";
        if (total.compareTo(BigDecimal.valueOf(75)) >= 0) return "B";
        if (total.compareTo(BigDecimal.valueOf(60)) >= 0) return "C";
        return "D";
    }

    private void checkAndSuspendIfTwoConsecutiveD(Long supplierId) {
        List<SupplierEvaluation> evals = evaluationRepository.findTop2BySupplierIdAndGradeOrderByEvaluatedAtDesc(supplierId, "D");
        if (evals != null && evals.size() >= 2) {
            List<SupplierEvaluation> allRecent = evaluationRepository.findBySupplierIdOrderByEvaluatedAtDesc(supplierId);
            if (allRecent.size() >= 2) {
                boolean twoD = "D".equals(allRecent.get(0).getGrade()) && "D".equals(allRecent.get(1).getGrade());
                if (twoD) {
                    supplierRepository.findById(supplierId).ifPresent(s -> {
                        s.setApprovalStatus("暂停合作");
                        s.setStatus("暂停合作");
                        supplierRepository.save(s);
                    });
                }
            }
        }
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("username") String username,
            @RequestBody SupplierEvaluation eval) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限，仅采购主管或管理员可发起评估"));
        }
        if (eval.getSupplierId() == null || eval.getEvaluationPeriod() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "供应商和评估周期为必填"));
        }
        if (!supplierRepository.existsById(eval.getSupplierId())) {
            return ResponseEntity.notFound().build();
        }
        Supplier supplier = supplierRepository.findById(eval.getSupplierId()).orElse(null);
        if (supplier != null && !"合格供应商".equals(supplier.getApprovalStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "仅合格供应商可进行评估"));
        }

        eval.setId(null);
        if (eval.getEvaluationType() == null) eval.setEvaluationType("季度");
        if (eval.getQualityScore() == null) {
            eval.setQualityScore(calculateScore(eval.getQualityQualifiedBatches(), eval.getQualityTotalBatches()));
        }
        if (eval.getDeliveryScore() == null) {
            eval.setDeliveryScore(calculateScore(eval.getDeliveryOnTimeBatches(), eval.getDeliveryTotalBatches()));
        }
        if (eval.getPriceScore() == null) eval.setPriceScore(BigDecimal.ZERO);
        if (eval.getServiceScore() == null) eval.setServiceScore(BigDecimal.ZERO);

        BigDecimal total = eval.getQualityScore().multiply(BigDecimal.valueOf(0.35))
                .add(eval.getDeliveryScore().multiply(BigDecimal.valueOf(0.25)))
                .add(eval.getPriceScore().multiply(BigDecimal.valueOf(0.20)))
                .add(eval.getServiceScore().multiply(BigDecimal.valueOf(0.20)))
                .setScale(2, RoundingMode.HALF_UP);
        eval.setTotalScore(total);
        eval.setGrade(calculateGrade(total));
        eval.setEvaluatorId(userId);
        eval.setEvaluatorName(username);
        eval.setEvaluatedAt(LocalDateTime.now());

        SupplierEvaluation saved = evaluationRepository.save(eval);

        if ("D".equals(saved.getGrade())) {
            checkAndSuspendIfTwoConsecutiveD(eval.getSupplierId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String evaluationPeriod,
            @RequestParam(required = false) String grade) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("evaluatedAt").descending());
        Page<SupplierEvaluation> result;
        if (supplierId != null) {
            result = evaluationRepository.findBySupplierId(supplierId, pr);
        } else {
            result = evaluationRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return evaluationRepository.findById(id)
                .map(e -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("evaluation", e);
                    supplierRepository.findById(e.getSupplierId()).ifPresent(s -> result.put("supplierName", s.getName()));
                    return ResponseEntity.ok((Object) result);
                }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<?> listBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(evaluationRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestAttribute("role") String role,
            @PathVariable Long id,
            @RequestBody SupplierEvaluation body) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        return evaluationRepository.findById(id).map(e -> {
            if (body.getQualityQualifiedBatches() != null) e.setQualityQualifiedBatches(body.getQualityQualifiedBatches());
            if (body.getQualityTotalBatches() != null) e.setQualityTotalBatches(body.getQualityTotalBatches());
            if (body.getDeliveryOnTimeBatches() != null) e.setDeliveryOnTimeBatches(body.getDeliveryOnTimeBatches());
            if (body.getDeliveryTotalBatches() != null) e.setDeliveryTotalBatches(body.getDeliveryTotalBatches());
            if (body.getQualityScore() != null) e.setQualityScore(body.getQualityScore());
            if (body.getDeliveryScore() != null) e.setDeliveryScore(body.getDeliveryScore());
            if (body.getPriceScore() != null) e.setPriceScore(body.getPriceScore());
            if (body.getServiceScore() != null) e.setServiceScore(body.getServiceScore());
            if (body.getEvaluationRemark() != null) e.setEvaluationRemark(body.getEvaluationRemark());

            if (body.getQualityScore() == null && (body.getQualityQualifiedBatches() != null || body.getQualityTotalBatches() != null)) {
                e.setQualityScore(calculateScore(e.getQualityQualifiedBatches(), e.getQualityTotalBatches()));
            }
            if (body.getDeliveryScore() == null && (body.getDeliveryOnTimeBatches() != null || body.getDeliveryTotalBatches() != null)) {
                e.setDeliveryScore(calculateScore(e.getDeliveryOnTimeBatches(), e.getDeliveryTotalBatches()));
            }

            BigDecimal total = e.getQualityScore().multiply(BigDecimal.valueOf(0.35))
                    .add(e.getDeliveryScore().multiply(BigDecimal.valueOf(0.25)))
                    .add(e.getPriceScore().multiply(BigDecimal.valueOf(0.20)))
                    .add(e.getServiceScore().multiply(BigDecimal.valueOf(0.20)))
                    .setScale(2, RoundingMode.HALF_UP);
            e.setTotalScore(total);
            e.setGrade(calculateGrade(total));

            SupplierEvaluation saved = evaluationRepository.save(e);

            if ("D".equals(saved.getGrade())) {
                checkAndSuspendIfTwoConsecutiveD(e.getSupplierId());
            }

            return ResponseEntity.ok((Object) saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestAttribute("role") String role,
            @PathVariable Long id) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        if (!evaluationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        evaluationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
