package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.service.FlowerVarietyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/varieties")
@RequiredArgsConstructor
public class FlowerVarietyController {

    private final FlowerVarietyService flowerVarietyService;

    @GetMapping
    public Result<List<FlowerVariety>> findAll() {
        return Result.success(flowerVarietyService.findAll());
    }

    @GetMapping("/{id}")
    public Result<FlowerVariety> findById(@PathVariable Long id) {
        FlowerVariety variety = flowerVarietyService.findById(id);
        if (variety == null) {
            return Result.fail("品种不存在");
        }
        return Result.success(variety);
    }

    @GetMapping("/category/{category}")
    public Result<List<FlowerVariety>> findByCategory(@PathVariable String category) {
        return Result.success(flowerVarietyService.findByCategory(category));
    }

    @PostMapping
    public Result<FlowerVariety> save(@RequestBody FlowerVariety variety) {
        return Result.success(flowerVarietyService.save(variety));
    }

    @PutMapping("/{id}")
    public Result<FlowerVariety> update(@PathVariable Long id, @RequestBody FlowerVariety variety) {
        variety.setId(id);
        return Result.success(flowerVarietyService.save(variety));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        flowerVarietyService.deleteById(id);
        return Result.success();
    }
}
