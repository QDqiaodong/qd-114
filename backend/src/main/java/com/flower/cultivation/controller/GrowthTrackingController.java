package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.BatchHealthDTO;
import com.flower.cultivation.dto.HealthStatusAggregationDTO;
import com.flower.cultivation.dto.TransplantReadinessDTO;
import com.flower.cultivation.dto.VarietyHealthDTO;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.service.GrowthTrackingService;
import com.flower.cultivation.service.HealthStatusAggregationService;
import com.flower.cultivation.service.TransplantReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/growth-trackings")
@RequiredArgsConstructor
public class GrowthTrackingController {

    private final GrowthTrackingService growthTrackingService;
    private final HealthStatusAggregationService healthStatusAggregationService;
    private final TransplantReadinessService transplantReadinessService;

    @GetMapping("/sowing/{sowingId}")
    public Result<List<GrowthTracking>> findBySowingId(@PathVariable Long sowingId) {
        return Result.success(growthTrackingService.findBySowingId(sowingId));
    }

    @GetMapping("/{id}")
    public Result<GrowthTracking> findById(@PathVariable Long id) {
        GrowthTracking tracking = growthTrackingService.findById(id);
        if (tracking == null) {
            return Result.fail("生长记录不存在");
        }
        return Result.success(tracking);
    }

    @PostMapping
    public Result<GrowthTracking> save(@RequestBody GrowthTracking tracking) {
        return Result.success(growthTrackingService.save(tracking));
    }

    @PutMapping("/{id}")
    public Result<GrowthTracking> update(@PathVariable Long id, @RequestBody GrowthTracking tracking) {
        tracking.setId(id);
        return Result.success(growthTrackingService.save(tracking));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        growthTrackingService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/health/aggregation")
    public Result<HealthStatusAggregationDTO> getHealthAggregation() {
        return Result.success(healthStatusAggregationService.getAggregation());
    }

    @GetMapping("/health/batch/{sowingId}")
    public Result<BatchHealthDTO> getBatchHealth(@PathVariable Long sowingId) {
        BatchHealthDTO batchHealth = healthStatusAggregationService.getBatchHealth(sowingId);
        if (batchHealth == null) {
            return Result.fail("未找到该批次的健康数据");
        }
        return Result.success(batchHealth);
    }

    @GetMapping("/health/variety/{varietyId}")
    public Result<VarietyHealthDTO> getVarietyHealth(@PathVariable Long varietyId) {
        VarietyHealthDTO varietyHealth = healthStatusAggregationService.getVarietyHealth(varietyId);
        if (varietyHealth == null) {
            return Result.fail("未找到该品种的健康数据");
        }
        return Result.success(varietyHealth);
    }

    @GetMapping("/health/batch/by-variety/{varietyId}")
    public Result<List<BatchHealthDTO>> getBatchHealthByVariety(@PathVariable Long varietyId) {
        return Result.success(healthStatusAggregationService.getBatchHealthByVariety(varietyId));
    }

    @GetMapping("/transplant-readiness/{sowingId}")
    public Result<TransplantReadinessDTO> getTransplantReadiness(@PathVariable Long sowingId) {
        TransplantReadinessDTO readiness = transplantReadinessService.assessReadiness(sowingId);
        if (readiness == null) {
            return Result.fail("未找到该播种记录的移栽就绪评估数据");
        }
        return Result.success(readiness);
    }

    @GetMapping("/transplant-readiness")
    public Result<List<TransplantReadinessDTO>> getAllTransplantReadiness() {
        return Result.success(transplantReadinessService.assessAllReadiness());
    }

    @PostMapping("/transplant-readiness/batch")
    public Result<List<TransplantReadinessDTO>> getBatchTransplantReadiness(@RequestBody List<Long> sowingIds) {
        return Result.success(transplantReadinessService.assessBatchReadiness(sowingIds));
    }
}
