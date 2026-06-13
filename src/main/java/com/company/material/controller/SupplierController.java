package com.company.material.controller;

import com.company.material.entity.Supplier;
import com.company.material.entity.SupplierQualification;
import com.company.material.entity.SupplierBlacklist;
import com.company.material.repository.SupplierRepository;
import com.company.material.repository.SupplierQualificationRepository;
import com.company.material.repository.SupplierBlacklistRepository;
import com.company.material.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final SupplierQualificationRepository qualificationRepository;
    private final SupplierBlacklistRepository blacklistRepository;
    private final UserRepository userRepository;

    private boolean isPurchasingManagerOrAdmin(String role) {
        return "管理员".equals(role) || "采购主管".equals(role);
    }

    private boolean isPurchasingStaff(String role) {
        return "管理员".equals(role) || "采购主管".equals(role) || "采购员".equals(role);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Supplier supplier) {
        if (supplier.getSupplierCode() == null || supplier.getName() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "供应商编码和名称为必填"));
        }
        if (supplierRepository.existsBySupplierCode(supplier.getSupplierCode())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "供应商编码已存在"));
        }
        supplier.setId(null);
        supplier.setApprovalStatus("待审核");
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierRepository.save(supplier));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) String keyword) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Supplier> result;
        if (approvalStatus != null && !approvalStatus.isBlank()) {
            result = supplierRepository.findByApprovalStatus(approvalStatus, pr);
        } else if (status != null && !status.isBlank()) {
            result = supplierRepository.findByStatus(status, pr);
        } else if (category != null && !category.isBlank()) {
            result = supplierRepository.findByCategory(category, pr);
        } else {
            result = supplierRepository.findAll(pr);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all-approved")
    public ResponseEntity<?> listApproved() {
        List<Supplier> result = supplierRepository.findByApprovalStatus("合格供应商");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return supplierRepository.findById(id)
                .map(s -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("supplier", s);
                    result.put("qualifications", qualificationRepository.findBySupplierId(id));
                    return ResponseEntity.ok((Object) result);
                }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Supplier body) {
        return supplierRepository.findById(id).map(s -> {
            if (body.getName() != null) s.setName(body.getName());
            if (body.getContactPerson() != null) s.setContactPerson(body.getContactPerson());
            if (body.getPhone() != null) s.setPhone(body.getPhone());
            if (body.getAddress() != null) s.setAddress(body.getAddress());
            if (body.getCategory() != null) s.setCategory(body.getCategory());
            if (body.getStatus() != null) s.setStatus(body.getStatus());
            if (body.getBusinessLicenseNo() != null) s.setBusinessLicenseNo(body.getBusinessLicenseNo());
            if (body.getTaxNo() != null) s.setTaxNo(body.getTaxNo());
            if (body.getBankName() != null) s.setBankName(body.getBankName());
            if (body.getBankAccount() != null) s.setBankAccount(body.getBankAccount());
            if (body.getRegisteredCapital() != null) s.setRegisteredCapital(body.getRegisteredCapital());
            if (body.getBusinessScope() != null) s.setBusinessScope(body.getBusinessScope());
            return ResponseEntity.ok((Object) supplierRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限，仅采购主管或管理员可审核"));
        }
        String action = body.get("action");
        String remark = body.get("remark");
        if (!"approve".equals(action) && !"reject".equals(action)) {
            return ResponseEntity.badRequest().body(Map.of("error", "操作无效"));
        }
        return supplierRepository.findById(id).map(s -> {
            if ("approve".equals(action)) {
                s.setApprovalStatus("合格供应商");
                s.setStatus("合作中");
            } else {
                s.setApprovalStatus("待审核");
            }
            s.setApprovalRemark(remark);
            s.setApprovedBy(userId);
            s.setApprovedAt(LocalDateTime.now());
            return ResponseEntity.ok((Object) supplierRepository.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{supplierId}/qualifications")
    public ResponseEntity<?> listQualifications(@PathVariable Long supplierId) {
        if (!supplierRepository.existsById(supplierId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(qualificationRepository.findBySupplierId(supplierId));
    }

    @PostMapping("/{supplierId}/qualifications")
    public ResponseEntity<?> addQualification(
            @PathVariable Long supplierId,
            @RequestBody SupplierQualification q) {
        if (!supplierRepository.existsById(supplierId)) {
            return ResponseEntity.notFound().build();
        }
        q.setId(null);
        q.setSupplierId(supplierId);
        return ResponseEntity.status(HttpStatus.CREATED).body(qualificationRepository.save(q));
    }

    @PutMapping("/qualifications/{id}")
    public ResponseEntity<?> updateQualification(
            @PathVariable Long id,
            @RequestBody SupplierQualification body) {
        return qualificationRepository.findById(id).map(q -> {
            if (body.getCertificateName() != null) q.setCertificateName(body.getCertificateName());
            if (body.getCertificateNo() != null) q.setCertificateNo(body.getCertificateNo());
            if (body.getValidFrom() != null) q.setValidFrom(body.getValidFrom());
            if (body.getValidTo() != null) q.setValidTo(body.getValidTo());
            if (body.getRemark() != null) q.setRemark(body.getRemark());
            return ResponseEntity.ok((Object) qualificationRepository.save(q));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/qualifications/{id}")
    public ResponseEntity<?> deleteQualification(@PathVariable Long id) {
        if (!qualificationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        qualificationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @PostMapping("/{supplierId}/blacklist")
    public ResponseEntity<?> addToBlacklist(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("username") String username,
            @PathVariable Long supplierId,
            @RequestBody Map<String, String> body) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限，仅采购主管或管理员可操作黑名单"));
        }
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "拉黑原因为必填"));
        }
        return supplierRepository.findById(supplierId).map(s -> {
            if (blacklistRepository.existsBySupplierIdAndStatus(supplierId, "生效中")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "该供应商已在黑名单中"));
            }
            SupplierBlacklist bl = new SupplierBlacklist();
            bl.setSupplierId(supplierId);
            bl.setBlacklistReason(reason);
            bl.setOperatorId(userId);
            bl.setOperatorName(username);
            blacklistRepository.save(bl);
            s.setApprovalStatus("黑名单");
            s.setStatus("黑名单");
            supplierRepository.save(s);
            return ResponseEntity.ok(Map.of("message", "已加入黑名单"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/blacklist/{id}/unblock")
    public ResponseEntity<?> unblockBlacklist(
            @RequestAttribute("role") String role,
            @RequestAttribute("userId") Long userId,
            @RequestAttribute("username") String username,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        if (!isPurchasingManagerOrAdmin(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权限"));
        }
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "解除原因为必填"));
        }
        return blacklistRepository.findById(id).map(bl -> {
            if (!"生效中".equals(bl.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "该黑名单记录非生效状态"));
            }
            bl.setStatus("已解除");
            bl.setUnblacklistReason(reason);
            bl.setUnblacklistApproverId(userId);
            bl.setUnblacklistApproverName(username);
            bl.setUnblacklistDate(LocalDateTime.now());
            blacklistRepository.save(bl);
            supplierRepository.findById(bl.getSupplierId()).ifPresent(s -> {
                s.setApprovalStatus("合格供应商");
                s.setStatus("合作中");
                supplierRepository.save(s);
            });
            return ResponseEntity.ok(Map.of("message", "已解除黑名单"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/blacklist")
    public ResponseEntity<?> listBlacklist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "生效中") String status) {
        PageRequest pr = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(blacklistRepository.findByStatus(status, pr));
    }
}
