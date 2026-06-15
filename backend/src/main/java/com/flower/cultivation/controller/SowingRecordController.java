package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.SowingRecordResult;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.service.SowingRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sowings")
@RequiredArgsConstructor
public class SowingRecordController {

    private final SowingRecordService sowingRecordService;

    @GetMapping
    public Result<List<SowingRecord>> findAll() {
        return Result.success(sowingRecordService.findAll());
    }

    @GetMapping("/{id}")
    public Result<SowingRecord> findById(@PathVariable Long id) {
        SowingRecord record = sowingRecordService.findById(id);
        if (record == null) {
            return Result.fail("播种记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/seed/{seedId}")
    public Result<List<SowingRecord>> findBySeedId(@PathVariable Long seedId) {
        return Result.success(sowingRecordService.findBySeedId(seedId));
    }

    @GetMapping("/variety/{varietyId}")
    public Result<List<SowingRecord>> findByVarietyId(@PathVariable Long varietyId) {
        return Result.success(sowingRecordService.findByVarietyId(varietyId));
    }

    @PostMapping
    public Result<SowingRecordResult> save(@RequestBody SowingRecord record) {
        try {
            return Result.success(sowingRecordService.save(record));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<SowingRecordResult> update(@PathVariable Long id, @RequestBody SowingRecord record) {
        record.setId(id);
        try {
            return Result.success(sowingRecordService.save(record));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            sowingRecordService.deleteById(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
