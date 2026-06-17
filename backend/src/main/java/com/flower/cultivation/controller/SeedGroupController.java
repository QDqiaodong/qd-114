package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.GroupMigrationPreviewDTO;
import com.flower.cultivation.dto.GroupSeedItemDTO;
import com.flower.cultivation.entity.SeedGroup;
import com.flower.cultivation.service.SeedGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/seed-groups")
@RequiredArgsConstructor
public class SeedGroupController {

    private final SeedGroupService seedGroupService;

    @GetMapping
    public Result<List<SeedGroup>> findAll() {
        return Result.success(seedGroupService.findAll());
    }

    @GetMapping("/{id}")
    public Result<SeedGroup> findById(@PathVariable Long id) {
        SeedGroup group = seedGroupService.findById(id);
        if (group == null) {
            return Result.fail("分组不存在");
        }
        return Result.success(group);
    }

    @GetMapping("/code/{groupCode}")
    public Result<SeedGroup> findByGroupCode(@PathVariable String groupCode) {
        SeedGroup group = seedGroupService.findByGroupCode(groupCode);
        if (group == null) {
            return Result.fail("分组不存在");
        }
        return Result.success(group);
    }

    @GetMapping("/category/{category}")
    public Result<List<SeedGroup>> findByCategory(@PathVariable String category) {
        return Result.success(seedGroupService.findByCategory(category));
    }

    @PostMapping
    public Result<SeedGroup> save(@RequestBody SeedGroup seedGroup) {
        try {
            return Result.success(seedGroupService.save(seedGroup));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<SeedGroup> update(@PathVariable Long id, @RequestBody SeedGroup seedGroup) {
        seedGroup.setId(id);
        try {
            return Result.success(seedGroupService.save(seedGroup));
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            seedGroupService.deleteById(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/{id}/seeds")
    public Result<List<GroupSeedItemDTO>> getGroupSeeds(@PathVariable Long id) {
        return Result.success(seedGroupService.getGroupSeeds(id));
    }

    @GetMapping("/migration-preview")
    public Result<GroupMigrationPreviewDTO> getMigrationPreview(
            @RequestParam Long seedId,
            @RequestParam Long targetGroupId) {
        try {
            GroupMigrationPreviewDTO preview = seedGroupService.getMigrationPreview(seedId, targetGroupId);
            return Result.success(preview);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/move-seed")
    public Result<Boolean> moveSeedToGroup(@RequestBody Map<String, Object> params) {
        try {
            Long seedId = Long.valueOf(params.get("seedId").toString());
            Long targetGroupId = Long.valueOf(params.get("targetGroupId").toString());
            Integer sortOrder = params.get("sortOrder") != null
                    ? Integer.valueOf(params.get("sortOrder").toString())
                    : null;
            boolean result = seedGroupService.moveSeedToGroup(seedId, targetGroupId, sortOrder);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/move-seeds")
    public Result<Boolean> moveSeedsToGroup(@RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> seedIds = ((List<Number>) params.get("seedIds")).stream()
                    .map(Number::longValue)
                    .toList();
            Long targetGroupId = Long.valueOf(params.get("targetGroupId").toString());
            boolean result = seedGroupService.moveSeedsToGroup(seedIds, targetGroupId);
            return Result.success(result);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
