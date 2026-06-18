package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.TransplantRecoveryDetailDTO;
import com.flower.cultivation.entity.TransplantRecoveryRecord;
import com.flower.cultivation.service.TransplantRecoveryRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transplant-recovery-records")
@RequiredArgsConstructor
public class TransplantRecoveryRecordController {

    private final TransplantRecoveryRecordService recoveryRecordService;

    @GetMapping("/transplant/{transplantId}")
    public Result<List<TransplantRecoveryRecord>> findByTransplantId(@PathVariable Long transplantId) {
        return Result.success(recoveryRecordService.findByTransplantId(transplantId));
    }

    @GetMapping("/transplant/{transplantId}/desc")
    public Result<List<TransplantRecoveryRecord>> findByTransplantIdDesc(@PathVariable Long transplantId) {
        return Result.success(recoveryRecordService.findByTransplantIdDesc(transplantId));
    }

    @GetMapping("/sowing/{sowingId}")
    public Result<List<TransplantRecoveryRecord>> findBySowingId(@PathVariable Long sowingId) {
        return Result.success(recoveryRecordService.findBySowingId(sowingId));
    }

    @GetMapping("/{id}")
    public Result<TransplantRecoveryRecord> findById(@PathVariable Long id) {
        TransplantRecoveryRecord record = recoveryRecordService.findById(id);
        if (record == null) {
            return Result.fail("恢复记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/transplant/{transplantId}/latest")
    public Result<TransplantRecoveryRecord> findLatestByTransplantId(@PathVariable Long transplantId) {
        return Result.success(recoveryRecordService.findLatestByTransplantId(transplantId));
    }

    @GetMapping("/transplant/{transplantId}/detail")
    public Result<TransplantRecoveryDetailDTO> getRecoveryDetail(@PathVariable Long transplantId) {
        TransplantRecoveryDetailDTO detail = recoveryRecordService.getRecoveryDetail(transplantId);
        if (detail == null) {
            return Result.fail("移栽记录不存在");
        }
        return Result.success(detail);
    }

    @PostMapping
    public Result<TransplantRecoveryRecord> save(@RequestBody TransplantRecoveryRecord record) {
        try {
            return Result.success(recoveryRecordService.save(record));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<TransplantRecoveryRecord> update(
            @PathVariable Long id,
            @RequestBody TransplantRecoveryRecord record) {
        record.setId(id);
        try {
            return Result.success(recoveryRecordService.save(record));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        recoveryRecordService.deleteById(id);
        return Result.success();
    }
}
