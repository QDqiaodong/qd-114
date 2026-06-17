package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.GrowthStageVersionDTO;
import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.service.GrowthStageCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stages")
@RequiredArgsConstructor
public class GrowthStageController {

    private final GrowthStageCacheService growthStageCacheService;

    @GetMapping
    public Result<List<GrowthStage>> findAll() {
        return Result.success(growthStageCacheService.getAllStages());
    }

    @GetMapping("/version")
    public Result<GrowthStageVersionDTO> getStagesWithVersion() {
        return Result.success(growthStageCacheService.getStagesWithVersion());
    }

    @PostMapping("/refresh")
    public Result<Void> refreshCache() {
        growthStageCacheService.refreshStagesCache();
        return Result.success();
    }
}
