package com.flower.cultivation.service;

import com.flower.cultivation.dto.SowingBatchReviewItem;
import com.flower.cultivation.dto.VarietyCardDTO;
import com.flower.cultivation.dto.VarietyReviewDTO;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SeedInfoRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowerVarietyService {

    private final FlowerVarietyRepository flowerVarietyRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SeedInfoRepository seedInfoRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final TransplantRecordRepository transplantRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;

    private static final String VARIETIES_CACHE_KEY = "flower:varieties:list";
    private static final long CACHE_TTL_HOURS = 24;

    @PostConstruct
    public void init() {
        try {
            refreshCache();
            log.info("花卉品种数据缓存初始化完成");
        } catch (Exception e) {
            log.warn("花卉品种缓存初始化失败，将在首次访问时加载: {}", e.getMessage());
        }
    }

    public List<FlowerVariety> findAll() {
        try {
            List<Object> cached = redisTemplate.opsForList().range(VARIETIES_CACHE_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .filter(obj -> obj instanceof FlowerVariety)
                        .map(obj -> (FlowerVariety) obj)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("从Redis获取品种数据失败，将从数据库加载: {}", e.getMessage());
        }

        List<FlowerVariety> varieties = flowerVarietyRepository.findAll();
        cacheVarieties(varieties);
        return varieties;
    }

    public FlowerVariety findById(Long id) {
        return flowerVarietyRepository.findById(id).orElse(null);
    }

    public List<FlowerVariety> findByCategory(String category) {
        return flowerVarietyRepository.findByCategory(category);
    }

    public FlowerVariety save(FlowerVariety variety) {
        FlowerVariety saved = flowerVarietyRepository.save(variety);
        refreshCache();
        return saved;
    }

    public void deleteById(Long id) {
        if (seedInfoRepository.existsByVarietyId(id)) {
            throw new RuntimeException("该品种存在种子信息引用，无法删除");
        }
        if (sowingRecordRepository.existsByVarietyId(id)) {
            throw new RuntimeException("该品种存在播种记录引用，无法删除");
        }
        if (transplantRecordRepository.existsByVarietyId(id)) {
            throw new RuntimeException("该品种存在移栽记录引用，无法删除");
        }
        flowerVarietyRepository.deleteById(id);
        refreshCache();
    }

    private void cacheVarieties(List<FlowerVariety> varieties) {
        try {
            redisTemplate.delete(VARIETIES_CACHE_KEY);
            if (varieties != null && !varieties.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(VARIETIES_CACHE_KEY, varieties.toArray());
                redisTemplate.expire(VARIETIES_CACHE_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.error("缓存品种数据失败: {}", e.getMessage());
        }
    }

    private void refreshCache() {
        List<FlowerVariety> varieties = flowerVarietyRepository.findAll();
        cacheVarieties(varieties);
    }

    public Map<String, List<VarietyCardDTO>> getVarietyCardWall() {
        List<FlowerVariety> allVarieties = flowerVarietyRepository.findAll();
        List<SeedInfo> allSeeds = seedInfoRepository.findAllByOrderByCreateTimeDesc();

        Map<Long, Integer> seedQuantityMap = new HashMap<>();
        for (SeedInfo seed : allSeeds) {
            seedQuantityMap.merge(seed.getVarietyId(), seed.getRemainingQuantity(), Integer::sum);
        }

        List<TransplantRecord> transplants = transplantRecordRepository.findAll();
        Set<Long> transplantedSowingIds = transplants.stream()
                .map(TransplantRecord::getSowingId)
                .collect(Collectors.toSet());

        List<SowingRecord> allSowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();
        Map<Long, Integer> activeSowingMap = new HashMap<>();
        for (SowingRecord sowing : allSowings) {
            if (!transplantedSowingIds.contains(sowing.getId())) {
                activeSowingMap.merge(sowing.getVarietyId(), 1, Integer::sum);
            }
        }

        Map<String, List<VarietyCardDTO>> result = new LinkedHashMap<>();
        for (FlowerVariety variety : allVarieties) {
            VarietyCardDTO card = new VarietyCardDTO();
            card.setVarietyId(variety.getId());
            card.setName(variety.getName());
            card.setAlias(variety.getAlias());
            card.setCategory(variety.getCategory());
            card.setGerminationDays(variety.getGerminationDays());
            card.setSeedlingDays(variety.getSeedlingDays());
            card.setSeedQuantity(seedQuantityMap.getOrDefault(variety.getId(), 0));
            card.setActiveSowingBatches(activeSowingMap.getOrDefault(variety.getId(), 0));

            String category = variety.getCategory() != null ? variety.getCategory() : "未分类";
            result.computeIfAbsent(category, k -> new ArrayList<>()).add(card);
        }

        return result;
    }

    public VarietyReviewDTO getVarietyReview(Long varietyId) {
        FlowerVariety variety = flowerVarietyRepository.findById(varietyId).orElse(null);
        if (variety == null) {
            return null;
        }

        VarietyReviewDTO review = new VarietyReviewDTO();
        review.setVarietyId(variety.getId());
        review.setName(variety.getName());
        review.setAlias(variety.getAlias());
        review.setCategory(variety.getCategory());
        review.setGerminationDays(variety.getGerminationDays());
        review.setSeedlingDays(variety.getSeedlingDays());
        review.setDescription(variety.getDescription());

        List<SowingRecord> sowings = sowingRecordRepository.findByVarietyId(varietyId);
        List<TransplantRecord> transplants = transplantRecordRepository.findByVarietyId(varietyId);

        review.setTotalSowingBatches(sowings.size());
        review.setTotalSowingSeeds(sowings.stream().mapToInt(SowingRecord::getSowingQuantity).sum());
        review.setTotalTransplants(transplants.size());
        review.setTotalTransplantQuantity(transplants.stream().mapToInt(t -> t.getTransplantQuantity() != null ? t.getTransplantQuantity() : 0).sum());

        Map<Long, TransplantRecord> transplantBySowingId = new HashMap<>();
        for (TransplantRecord t : transplants) {
            transplantBySowingId.put(t.getSowingId(), t);
        }

        List<SowingBatchReviewItem> batchItems = new ArrayList<>();
        List<Integer> germinationDayList = new ArrayList<>();
        int totalSeedsSown = 0;
        int totalSurvivalEstimate = 0;

        for (SowingRecord sowing : sowings) {
            SowingBatchReviewItem item = new SowingBatchReviewItem();
            item.setSowingId(sowing.getId());
            item.setSowingTime(sowing.getSowingTime());
            item.setSowingQuantity(sowing.getSowingQuantity());
            item.setSoilRatio(sowing.getSoilRatio());
            item.setContainerType(sowing.getContainerType());

            List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowing.getId());

            List<String> stageNames = trackings.stream()
                    .map(GrowthTracking::getStageName)
                    .distinct()
                    .collect(Collectors.toList());
            item.setStageRecords(stageNames);

            GrowthTracking sprouting = trackings.stream()
                    .filter(t -> "SPROUTING".equals(t.getStageCode()))
                    .findFirst()
                    .orElse(null);
            if (sprouting != null && sowing.getSowingTime() != null) {
                long days = Duration.between(sowing.getSowingTime(), sprouting.getRecordTime()).toDays();
                item.setActualGerminationDays((int) days);
                germinationDayList.add((int) days);
            }

            int latestSurvival = 0;
            for (GrowthTracking t : trackings) {
                if (t.getEstimatedSurvival() != null && t.getEstimatedSurvival() > latestSurvival) {
                    latestSurvival = t.getEstimatedSurvival();
                }
            }
            item.setEstimatedSurvival(latestSurvival);

            if (sowing.getSowingQuantity() != null && sowing.getSowingQuantity() > 0) {
                BigDecimal rate = BigDecimal.valueOf(latestSurvival)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(sowing.getSowingQuantity()), 1, RoundingMode.HALF_UP);
                item.setSeedlingRate(rate);
            }

            TransplantRecord tp = transplantBySowingId.get(sowing.getId());
            if (tp != null) {
                item.setHasTransplant(true);
                item.setTransplantQuantity(tp.getTransplantQuantity());
                item.setTransplantTime(tp.getTransplantTime());
            } else {
                item.setHasTransplant(false);
            }

            batchItems.add(item);

            totalSeedsSown += sowing.getSowingQuantity() != null ? sowing.getSowingQuantity() : 0;
            totalSurvivalEstimate += latestSurvival;
        }

        batchItems.sort((a, b) -> {
            if (a.getSowingTime() == null && b.getSowingTime() == null) return 0;
            if (a.getSowingTime() == null) return 1;
            if (b.getSowingTime() == null) return -1;
            return b.getSowingTime().compareTo(a.getSowingTime());
        });

        review.setSowingBatches(batchItems);

        if (!germinationDayList.isEmpty()) {
            double avg = germinationDayList.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            review.setAverageGerminationDays(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        }

        if (totalSeedsSown > 0) {
            BigDecimal overallRate = BigDecimal.valueOf(totalSurvivalEstimate)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalSeedsSown), 1, RoundingMode.HALF_UP);
            review.setSeedlingRate(overallRate);
        }

        return review;
    }
}
