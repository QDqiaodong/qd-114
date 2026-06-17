package com.flower.cultivation.service;

import com.flower.cultivation.dto.ConsumptionDistributionDTO;
import com.flower.cultivation.dto.GroupMigrationPreviewDTO;
import com.flower.cultivation.dto.GroupSeedItemDTO;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.SeedGroup;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.SeedGroupRepository;
import com.flower.cultivation.repository.SeedInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeedGroupService {

    private final SeedGroupRepository seedGroupRepository;
    private final SeedInfoRepository seedInfoRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;

    public List<SeedGroup> findAll() {
        return seedGroupRepository.findAllByOrderBySortOrderAsc();
    }

    public SeedGroup findById(Long id) {
        return seedGroupRepository.findById(id).orElse(null);
    }

    public SeedGroup findByGroupCode(String groupCode) {
        return seedGroupRepository.findByGroupCode(groupCode).orElse(null);
    }

    public List<SeedGroup> findByCategory(String category) {
        return seedGroupRepository.findByCategoryOrderBySortOrderAsc(category);
    }

    @Transactional
    public SeedGroup save(SeedGroup seedGroup) {
        if (seedGroup.getId() == null && seedGroup.getSortOrder() == null) {
            List<SeedGroup> allGroups = seedGroupRepository.findAllByOrderBySortOrderAsc();
            seedGroup.setSortOrder(allGroups.size() + 1);
        }
        return seedGroupRepository.save(seedGroup);
    }

    @Transactional
    public void deleteById(Long id) {
        if (seedInfoRepository.existsByGroupId(id)) {
            throw new RuntimeException("该分组下存在种子，无法删除");
        }
        seedGroupRepository.deleteById(id);
    }

    public List<GroupSeedItemDTO> getGroupSeeds(Long groupId) {
        List<SeedInfo> seeds = seedInfoRepository.findByGroupIdOrderBySortOrderAsc(groupId);
        return convertToGroupSeedItems(seeds);
    }

    public GroupMigrationPreviewDTO getMigrationPreview(Long seedId, Long targetGroupId) {
        SeedInfo seed = seedInfoRepository.findById(seedId).orElse(null);
        if (seed == null) {
            throw new RuntimeException("种子不存在");
        }

        SeedGroup targetGroup = seedGroupRepository.findById(targetGroupId).orElse(null);
        if (targetGroup == null) {
            throw new RuntimeException("目标分组不存在");
        }

        GroupMigrationPreviewDTO preview = new GroupMigrationPreviewDTO();
        preview.setTargetGroupId(targetGroup.getId());
        preview.setTargetGroupName(targetGroup.getGroupName());
        preview.setTargetGroupCode(targetGroup.getGroupCode());
        preview.setTargetCategory(targetGroup.getCategory());
        preview.setTargetLocation(targetGroup.getLocation());
        preview.setTargetSortOrder(targetGroup.getSortOrder());

        List<SeedInfo> existingSeeds = seedInfoRepository.findByGroupIdOrderBySortOrderAsc(targetGroupId);
        preview.setTargetSeedCount(existingSeeds.size());
        preview.setTargetTotalQuantity(
                existingSeeds.stream()
                        .mapToInt(s -> s.getRemainingQuantity() != null ? s.getRemainingQuantity() : 0)
                        .sum()
        );

        preview.setExistingSeeds(convertToGroupSeedItems(existingSeeds));

        preview.setConsumptionDistribution(buildConsumptionDistribution(existingSeeds));

        FlowerVariety seedVariety = flowerVarietyRepository.findById(seed.getVarietyId()).orElse(null);
        String seedCategory = seedVariety != null ? seedVariety.getCategory() : null;
        boolean categoryMatch = targetGroup.getCategory() == null
                || seedCategory == null
                || targetGroup.getCategory().equals(seedCategory);
        preview.setCategoryMatch(categoryMatch);

        List<String> warnings = new ArrayList<>();
        if (!categoryMatch) {
            warnings.add(String.format("分类不匹配：种子分类为【%s】，目标分组分类为【%s】，不建议混放",
                    seedCategory, targetGroup.getCategory()));
            preview.setCategoryWarning("分类不匹配，不建议混放不同类型的种子");
        } else {
            preview.setCategoryWarning(null);
        }

        Set<String> existingCategories = existingSeeds.stream()
                .map(s -> {
                    FlowerVariety v = flowerVarietyRepository.findById(s.getVarietyId()).orElse(null);
                    return v != null ? v.getCategory() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (existingCategories.size() > 1) {
            warnings.add("目标分组内存在多种分类的种子，建议整理");
        }

        preview.setWarnings(warnings);

        int suggestedSortOrder = calculateSuggestedSortOrder(existingSeeds, seed);
        preview.setSuggestedSortOrder(suggestedSortOrder);

        return preview;
    }

    private List<GroupSeedItemDTO> convertToGroupSeedItems(List<SeedInfo> seeds) {
        Map<Long, FlowerVariety> varietyMap = flowerVarietyRepository.findAll().stream()
                .collect(Collectors.toMap(FlowerVariety::getId, v -> v));

        return seeds.stream().map(seed -> {
            GroupSeedItemDTO item = new GroupSeedItemDTO();
            item.setSeedId(seed.getId());
            item.setVarietyName(seed.getVarietyName());
            item.setVarietyId(seed.getVarietyId());
            item.setSourceType(seed.getSourceType());
            item.setSourceTypeName("PURCHASE".equals(seed.getSourceType()) ? "购入" : "采收");
            item.setAcquireTime(seed.getAcquireTime());
            item.setRemainingQuantity(seed.getRemainingQuantity());
            item.setInitialQuantity(seed.getInitialQuantity());
            item.setSortOrder(seed.getSortOrder());

            FlowerVariety variety = varietyMap.get(seed.getVarietyId());
            if (variety != null) {
                item.setCategory(variety.getCategory());
            }

            if (seed.getInitialQuantity() != null && seed.getInitialQuantity() > 0
                    && seed.getRemainingQuantity() != null) {
                int consumed = seed.getInitialQuantity() - seed.getRemainingQuantity();
                BigDecimal rate = BigDecimal.valueOf(consumed)
                        .divide(BigDecimal.valueOf(seed.getInitialQuantity()), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                item.setConsumptionRate(rate);
            } else {
                item.setConsumptionRate(BigDecimal.ZERO);
            }

            return item;
        }).collect(Collectors.toList());
    }

    private List<ConsumptionDistributionDTO> buildConsumptionDistribution(List<SeedInfo> seeds) {
        Map<String, List<SeedInfo>> categoryMap = new LinkedHashMap<>();
        Map<Long, FlowerVariety> varietyMap = flowerVarietyRepository.findAll().stream()
                .collect(Collectors.toMap(FlowerVariety::getId, v -> v));

        for (SeedInfo seed : seeds) {
            FlowerVariety variety = varietyMap.get(seed.getVarietyId());
            String category = variety != null ? variety.getCategory() : "未分类";
            categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(seed);
        }

        List<ConsumptionDistributionDTO> distribution = new ArrayList<>();
        for (Map.Entry<String, List<SeedInfo>> entry : categoryMap.entrySet()) {
            ConsumptionDistributionDTO dto = new ConsumptionDistributionDTO();
            dto.setCategory(entry.getKey());
            dto.setCategoryName(entry.getKey());
            dto.setSeedCount(entry.getValue().size());

            int totalRemaining = entry.getValue().stream()
                    .mapToInt(s -> s.getRemainingQuantity() != null ? s.getRemainingQuantity() : 0)
                    .sum();
            int totalInitial = entry.getValue().stream()
                    .mapToInt(s -> s.getInitialQuantity() != null ? s.getInitialQuantity() : 0)
                    .sum();
            dto.setTotalRemainingQuantity(totalRemaining);
            dto.setTotalInitialQuantity(totalInitial);

            if (totalInitial > 0) {
                double rate = (double) (totalInitial - totalRemaining) / totalInitial * 100;
                dto.setConsumptionRate(rate);
            } else {
                dto.setConsumptionRate(0.0);
            }

            distribution.add(dto);
        }

        distribution.sort((a, b) -> b.getSeedCount().compareTo(a.getSeedCount()));
        return distribution;
    }

    private int calculateSuggestedSortOrder(List<SeedInfo> existingSeeds, SeedInfo movingSeed) {
        if (existingSeeds.isEmpty()) {
            return 1;
        }

        String movingVarietyName = movingSeed.getVarietyName();
        int insertPosition = 1;

        for (int i = 0; i < existingSeeds.size(); i++) {
            SeedInfo existing = existingSeeds.get(i);
            if (existing.getVarietyName().compareTo(movingVarietyName) < 0) {
                insertPosition = i + 2;
            }
        }

        return insertPosition;
    }

    @Transactional
    public boolean moveSeedToGroup(Long seedId, Long targetGroupId, Integer sortOrder) {
        SeedInfo seed = seedInfoRepository.findById(seedId).orElse(null);
        if (seed == null) {
            throw new RuntimeException("种子不存在");
        }

        SeedGroup targetGroup = seedGroupRepository.findById(targetGroupId).orElse(null);
        if (targetGroup == null) {
            throw new RuntimeException("目标分组不存在");
        }

        seed.setGroupId(targetGroupId);

        if (sortOrder != null) {
            seed.setSortOrder(sortOrder);
        } else {
            List<SeedInfo> groupSeeds = seedInfoRepository.findByGroupIdOrderBySortOrderAsc(targetGroupId);
            int maxSortOrder = groupSeeds.stream()
                    .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                    .max()
                    .orElse(0);
            seed.setSortOrder(maxSortOrder + 1);
        }

        seedInfoRepository.save(seed);
        return true;
    }

    @Transactional
    public boolean moveSeedsToGroup(List<Long> seedIds, Long targetGroupId) {
        SeedGroup targetGroup = seedGroupRepository.findById(targetGroupId).orElse(null);
        if (targetGroup == null) {
            throw new RuntimeException("目标分组不存在");
        }

        List<SeedInfo> groupSeeds = seedInfoRepository.findByGroupIdOrderBySortOrderAsc(targetGroupId);
        int currentMaxSort = groupSeeds.stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max()
                .orElse(0);

        for (Long seedId : seedIds) {
            SeedInfo seed = seedInfoRepository.findById(seedId).orElse(null);
            if (seed != null) {
                seed.setGroupId(targetGroupId);
                seed.setSortOrder(++currentMaxSort);
                seedInfoRepository.save(seed);
            }
        }

        return true;
    }
}
