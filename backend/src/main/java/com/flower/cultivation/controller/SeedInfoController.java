package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.SeedDetailDTO;
import com.flower.cultivation.dto.SeedRiskReportDTO;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.service.SeedInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seeds")
@RequiredArgsConstructor
public class SeedInfoController {

    private final SeedInfoService seedInfoService;

    @GetMapping
    public Result<List<SeedInfo>> findAll() {
        return Result.success(seedInfoService.findAll());
    }

    @GetMapping("/{id}")
    public Result<SeedInfo> findById(@PathVariable Long id) {
        SeedInfo seed = seedInfoService.findById(id);
        if (seed == null) {
            return Result.fail("种子信息不存在");
        }
        return Result.success(seed);
    }

    @GetMapping("/{id}/detail")
    public Result<SeedDetailDTO> getSeedDetail(@PathVariable Long id) {
        SeedDetailDTO detail = seedInfoService.getSeedDetail(id);
        if (detail == null) {
            return Result.fail("种子信息不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/variety/{varietyId}")
    public Result<List<SeedInfo>> findByVarietyId(@PathVariable Long varietyId) {
        return Result.success(seedInfoService.findByVarietyId(varietyId));
    }

    @GetMapping("/shelf-life-risk")
    public Result<SeedRiskReportDTO> getShelfLifeRisk() {
        return Result.success(seedInfoService.getShelfLifeRisk());
    }

    @PostMapping
    public Result<SeedInfo> save(@RequestBody SeedInfo seedInfo) {
        return Result.success(seedInfoService.save(seedInfo));
    }

    @PutMapping("/{id}")
    public Result<SeedInfo> update(@PathVariable Long id, @RequestBody SeedInfo seedInfo) {
        seedInfo.setId(id);
        return Result.success(seedInfoService.save(seedInfo));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            seedInfoService.deleteById(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
