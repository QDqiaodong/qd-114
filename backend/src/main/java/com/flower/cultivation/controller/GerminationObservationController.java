package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.entity.GerminationObservation;
import com.flower.cultivation.service.GerminationObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/germination-observations")
@RequiredArgsConstructor
public class GerminationObservationController {

    private final GerminationObservationService germinationObservationService;

    @GetMapping("/sowing/{sowingId}")
    public Result<List<GerminationObservation>> findBySowingId(@PathVariable Long sowingId) {
        return Result.success(germinationObservationService.findBySowingId(sowingId));
    }

    @GetMapping("/variety/{varietyId}")
    public Result<List<GerminationObservation>> findByVarietyId(@PathVariable Long varietyId) {
        return Result.success(germinationObservationService.findByVarietyId(varietyId));
    }

    @GetMapping("/{id}")
    public Result<GerminationObservation> findById(@PathVariable Long id) {
        GerminationObservation observation = germinationObservationService.findById(id);
        if (observation == null) {
            return Result.fail("发芽观察记录不存在");
        }
        return Result.success(observation);
    }

    @PostMapping
    public Result<GerminationObservation> save(@RequestBody GerminationObservation observation) {
        try {
            return Result.success(germinationObservationService.save(observation));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<GerminationObservation> update(@PathVariable Long id, @RequestBody GerminationObservation observation) {
        observation.setId(id);
        try {
            return Result.success(germinationObservationService.save(observation));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        germinationObservationService.deleteById(id);
        return Result.success();
    }
}
