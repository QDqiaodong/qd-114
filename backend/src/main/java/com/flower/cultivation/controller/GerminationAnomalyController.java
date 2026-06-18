package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.entity.GerminationAnomaly;
import com.flower.cultivation.service.GerminationAnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/germination-anomalies")
@RequiredArgsConstructor
public class GerminationAnomalyController {

    private final GerminationAnomalyService germinationAnomalyService;

    @GetMapping("/sowing/{sowingId}")
    public Result<List<GerminationAnomaly>> findBySowingId(@PathVariable Long sowingId) {
        return Result.success(germinationAnomalyService.findBySowingId(sowingId));
    }

    @GetMapping("/variety/{varietyId}")
    public Result<List<GerminationAnomaly>> findByVarietyId(@PathVariable Long varietyId) {
        return Result.success(germinationAnomalyService.findByVarietyId(varietyId));
    }

    @GetMapping("/status/{status}")
    public Result<List<GerminationAnomaly>> findByStatus(@PathVariable String status) {
        return Result.success(germinationAnomalyService.findByStatus(status));
    }

    @GetMapping("/{id}")
    public Result<GerminationAnomaly> findById(@PathVariable Long id) {
        GerminationAnomaly anomaly = germinationAnomalyService.findById(id);
        if (anomaly == null) {
            return Result.fail("异常记录不存在");
        }
        return Result.success(anomaly);
    }

    @GetMapping("/observation/{observationId}")
    public Result<GerminationAnomaly> findByObservationId(@PathVariable Long observationId) {
        GerminationAnomaly anomaly = germinationAnomalyService.findByObservationId(observationId);
        return Result.success(anomaly);
    }

    @GetMapping("/count/{status}")
    public Result<Long> countByStatus(@PathVariable String status) {
        return Result.success(germinationAnomalyService.countByStatus(status));
    }

    @PutMapping("/{id}/process")
    public Result<GerminationAnomaly> processAnomaly(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String actionTaken = request.get("actionTaken");
            String handler = request.get("handler");
            return Result.success(germinationAnomalyService.processAnomaly(id, actionTaken, handler));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/resolve")
    public Result<GerminationAnomaly> resolveAnomaly(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String resultNote = request.get("resultNote");
            String handler = request.get("handler");
            return Result.success(germinationAnomalyService.resolveAnomaly(id, resultNote, handler));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}/close")
    public Result<GerminationAnomaly> closeAnomaly(@PathVariable Long id) {
        try {
            return Result.success(germinationAnomalyService.closeAnomaly(id));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        germinationAnomalyService.deleteById(id);
        return Result.success();
    }
}
