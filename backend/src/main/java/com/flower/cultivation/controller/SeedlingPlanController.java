package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.entity.SeedlingPlan;
import com.flower.cultivation.service.SeedlingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/seedling-plans")
@RequiredArgsConstructor
public class SeedlingPlanController {

    private final SeedlingPlanService seedlingPlanService;

    @GetMapping
    public Result<List<SeedlingPlan>> findAll() {
        return Result.success(seedlingPlanService.findAll());
    }

    @GetMapping("/{id}")
    public Result<SeedlingPlan> findById(@PathVariable Long id) {
        SeedlingPlan plan = seedlingPlanService.findById(id);
        if (plan == null) {
            return Result.fail("育苗计划不存在");
        }
        return Result.success(plan);
    }

    @GetMapping("/seed/{seedId}")
    public Result<List<SeedlingPlan>> findBySeedId(@PathVariable Long seedId) {
        return Result.success(seedlingPlanService.findBySeedId(seedId));
    }

    @GetMapping("/variety/{varietyId}")
    public Result<List<SeedlingPlan>> findByVarietyId(@PathVariable Long varietyId) {
        return Result.success(seedlingPlanService.findByVarietyId(varietyId));
    }

    @GetMapping("/status/{status}")
    public Result<List<SeedlingPlan>> findByStatus(@PathVariable String status) {
        return Result.success(seedlingPlanService.findByStatus(status));
    }

    @PostMapping
    public Result<SeedlingPlan> save(@RequestBody SeedlingPlan plan) {
        try {
            return Result.success(seedlingPlanService.save(plan));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<SeedlingPlan> update(@PathVariable Long id, @RequestBody SeedlingPlan plan) {
        plan.setId(id);
        try {
            return Result.success(seedlingPlanService.save(plan));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public Result<SeedlingPlan> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate actualDate) {
        try {
            return Result.success(seedlingPlanService.updateStatus(id, status, actualDate));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        seedlingPlanService.deleteById(id);
        return Result.success();
    }
}
