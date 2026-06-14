package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.service.GrowthTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/growth-trackings")
@RequiredArgsConstructor
public class GrowthTrackingController {

    private final GrowthTrackingService growthTrackingService;

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
}
