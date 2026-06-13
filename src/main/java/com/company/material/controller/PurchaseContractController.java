package com.company.material.controller;

import com.company.material.entity.ContractExecution;
import com.company.material.entity.PurchaseContract;
import com.company.material.entity.SupplierBlacklist;
import com.company.material.repository.ContractExecutionRepository;
import com.company.material.repository.PurchaseContractRepository;
import com.company.material.repository.SupplierBlacklistRepository;
import com.company.material.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/purchase-contracts")
@RequiredArgsConstructor
public class PurchaseContractController {

    private final PurchaseContractRepository contractRepository;
    private final ContractExecutionRepository executionRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierBlacklistRepository blacklistRepository;

    private boolean isPurchasingStaff(String role) {
        return "管理员".equals(role) || "采购主管".equals(role) || "采购员".equals(role);
    }

    private boolean isPurchasingManagerOrAdmin(String role) {
        return "管理员".equals(role) || "采购主管".equals(role);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @RequestBody PurchaseContract contract) {
        if (!isPurchasingStaff(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        if (contract.getContractNo() == null || contract.getContractName() == null || contract.getSupplierId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "合同编号、名称、供应商为必填"));
        }
        if (contractRepository.existsByContractNo(contract.getContractNo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "合同编号已存在"));
        }
        if (!supplierRepository.existsById(contract.getSupplierId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "供应商不存在"));
        }
        if (blacklistRepository.existsBySupplierIdAndStatus(contract.getSupplierId(), "生效中")) {
            return ResponseEntity.badRequest().body(Map.of("error", "该供应商已被加入黑名单，无法新建合同"));
        }
        contract.setId(null);
        contract.setCreatedBy(userId);
        if (contract.getExecutedAmount() == null) contract.setExecutedAmount(BigDecimal.ZERO);
        return ResponseEntity.status(HttpStatus.CREATED).body(contractRepository.save(contract));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String contractStatus,
            @RequestParam(required = false) String contractType) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PurchaseContract> result;
        if (supplierId != null) {
            result = contractRepository.findBySupplierId(supplierId, pr);
        } else if (contractStatus != null && !contractStatus.isBlank()) {
            result = contractRepository.findByContractStatus(contractStatus, pr);
        } else {
            result = contractRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return contractRepository.findById(id).map(c -> {
            Map<String, Object> result = new HashMap<>();
            result.put("contract", c);
            supplierRepository.findById(c.getSupplierId()).ifPresent(s -> {
                result.put("supplierName", s.getName());
                result.put("supplierCode", s.getSupplierCode());
            });
            List<ContractExecution> executions = executionRepository.findByContractIdOrderByExecutionDateDesc(id);
            result.put("executions", executions);
            BigDecimal progress = BigDecimal.ZERO;
            if (c.getContractAmount() != null && c.getContractAmount().compareTo(BigDecimal.ZERO) > 0) {
                progress = c.getExecutedAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(c.getContractAmount(), 2, java.math.RoundingMode.HALF_UP);
            }
            result.put("progress", progress);
            boolean overExecuted = c.getContractAmount() != null
                    && c.getExecutedAmount().compareTo(c.getContractAmount()) > 0;
            result.put("overExecuted", overExecuted);
            return ResponseEntity.ok((Object) result);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<?> listBySupplier(@PathVariable Long supplierId) {
        return ResponseEntity.ok(contractRepository.findBySupplierId(supplierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @RequestAttribute("role") String role,
            @PathVariable Long id,
            @RequestBody PurchaseContract body) {
        if (!isPurchasingStaff(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        return contractRepository.findById(id).map(c -> {
            if (body.getContractName() != null) c.setContractName(body.getContractName());
            if (body.getSupplierId() != null) c.setSupplierId(body.getSupplierId());
            if (body.getContractAmount() != null) c.setContractAmount(body.getContractAmount());
            if (body.getSignDate() != null) c.setSignDate(body.getSignDate());
            if (body.getEffectiveDate() != null) c.setEffectiveDate(body.getEffectiveDate());
            if (body.getExpiryDate() != null) c.setExpiryDate(body.getExpiryDate());
            if (body.getPaymentTerms() != null) c.setPaymentTerms(body.getPaymentTerms());
            if (body.getContractType() != null) c.setContractType(body.getContractType());
            if (body.getContractStatus() != null) c.setContractStatus(body.getContractStatus());
            if (body.getRemark() != null) c.setRemark(body.getRemark());
            return ResponseEntity.ok((Object) contractRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @RequestAttribute("role") String role,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        String status = body.get("status");
        if (status == null || !List.of("草稿", "履行中", "已到期", "已终止").contains(status)) {
            return ResponseEntity.badRequest().body(Map.of("error", "状态值无效"));
        }
        return contractRepository.findById(id).map(c -> {
            c.setContractStatus(status);
            return ResponseEntity.ok((Object) contractRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{contractId}/executions")
    public ResponseEntity<?> addExecution(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("username") String username,
            @PathVariable Long contractId,
            @RequestBody ContractExecution execution) {
        if (!isPurchasingStaff(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        Optional<PurchaseContract> optContract = contractRepository.findById(contractId);
        if (optContract.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        PurchaseContract c = optContract.get();
        if (execution.getExecutionAmount() == null || execution.getExecutionDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "执行金额和日期为必填"));
        }
        execution.setId(null);
        execution.setContractId(contractId);
        execution.setOperatorId(userId);
        execution.setOperatorName(username);
        ContractExecution saved = executionRepository.save(execution);

        c.setExecutedAmount(c.getExecutedAmount().add(execution.getExecutionAmount()));
        contractRepository.save(c);

        Map<String, Object> result = new HashMap<>();
        result.put("execution", saved);
        if (c.getContractAmount() != null && c.getExecutedAmount().compareTo(c.getContractAmount()) > 0) {
            result.put("warning", "合同已超额执行！已执行: " + c.getExecutedAmount() + "，合同金额: " + c.getContractAmount());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{contractId}/executions")
    public ResponseEntity<?> listExecutions(@PathVariable Long contractId) {
        if (!contractRepository.existsById(contractId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(executionRepository.findByContractIdOrderByExecutionDateDesc(contractId));
    }

    @DeleteMapping("/executions/{id}")
    public ResponseEntity<?> deleteExecution(
            @RequestAttribute("role") String role,
            @PathVariable Long id) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        return executionRepository.findById(id).map(e -> {
            executionRepository.delete(e);
            contractRepository.findById(e.getContractId()).ifPresent(c -> {
                c.setExecutedAmount(c.getExecutedAmount().subtract(e.getExecutionAmount()));
                if (c.getExecutedAmount().compareTo(BigDecimal.ZERO) < 0) {
                    c.setExecutedAmount(BigDecimal.ZERO);
                }
                contractRepository.save(c);
            });
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @RequestAttribute("role") String role,
            @PathVariable Long id) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        if (!contractRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        contractRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
