package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        return Result.success(dashboardService.getDashboardStats());
    }

    @GetMapping("/germination-progress")
    public Result<List<Map<String, Object>>> getGerminationProgress() {
        return Result.success(dashboardService.getGerminationProgress());
    }

    @GetMapping("/growth-timeline")
    public Result<List<Map<String, Object>>> getGrowthTimeline() {
        return Result.success(dashboardService.getGrowthTimeline());
    }

    @GetMapping("/seed-vitality-calendar")
    public Result<Map<String, Object>> getSeedVitalityCalendar() {
        return Result.success(dashboardService.getSeedVitalityCalendar());
    }
}
