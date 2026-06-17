package com.flower.cultivation.service;

import com.flower.cultivation.dto.VarietyCardDTO;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.SeedInfoRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
}
