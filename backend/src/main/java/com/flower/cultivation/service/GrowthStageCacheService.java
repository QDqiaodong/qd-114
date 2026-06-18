package com.flower.cultivation.service;

import com.flower.cultivation.dto.GrowthStageVersionDTO;
import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.repository.GrowthStageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthStageCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GrowthStageRepository growthStageRepository;

    private static final String STAGES_CACHE_KEY = "growth:stages:list";
    private static final String STAGES_VERSION_KEY = "growth:stages:version";
    private static final String STAGES_UPDATE_TIME_KEY = "growth:stages:updateTime";
    private static final long CACHE_TTL_HOURS = 24;

    @PostConstruct
    public void init() {
        try {
            refreshStagesCache();
            log.info("生长阶段数据缓存初始化完成");
        } catch (Exception e) {
            log.warn("生长阶段缓存初始化失败，将在首次访问时加载: {}", e.getMessage());
        }
    }

    public List<GrowthStage> getAllStages() {
        try {
            List<Object> cached = redisTemplate.opsForList().range(STAGES_CACHE_KEY, 0, -1);
            if (cached != null && !cached.isEmpty()) {
                return cached.stream()
                        .filter(obj -> obj instanceof GrowthStage)
                        .map(obj -> (GrowthStage) obj)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("从Redis获取生长阶段失败，将从数据库加载: {}", e.getMessage());
        }

        List<GrowthStage> stages = growthStageRepository.findAllByOrderByStageOrderAsc();
        cacheStages(stages);
        return stages;
    }

    public GrowthStage getStageByCode(String stageCode) {
        if (stageCode == null || stageCode.isEmpty()) {
            return null;
        }
        try {
            List<GrowthStage> allStages = getAllStages();
            return allStages.stream()
                    .filter(s -> stageCode.equals(s.getStageCode()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("通过stageCode获取阶段失败，尝试直接从数据库查询: {}", e.getMessage());
            try {
                return growthStageRepository.findByStageCode(stageCode).orElse(null);
            } catch (Exception ex) {
                log.error("从数据库查询阶段失败: {}", ex.getMessage());
                return null;
            }
        }
    }

    public String getStageName(String stageCode, String fallbackName) {
        if (stageCode == null || stageCode.isEmpty()) {
            return buildFallbackStageName(null, fallbackName);
        }
        GrowthStage stage = getStageByCode(stageCode);
        if (stage != null && stage.getStageName() != null && !stage.getStageName().isEmpty()) {
            return stage.getStageName();
        }
        return buildFallbackStageName(stageCode, fallbackName);
    }

    private String buildFallbackStageName(String stageCode, String fallbackName) {
        if (fallbackName != null && !fallbackName.isEmpty()) {
            return fallbackName;
        }
        if (stageCode != null && !stageCode.isEmpty()) {
            return "未知阶段(" + stageCode + ")";
        }
        return "未知阶段";
    }

    public GrowthStageVersionDTO getStagesWithVersion() {
        GrowthStageVersionDTO versionDTO = new GrowthStageVersionDTO();

        try {
            Object versionObj = redisTemplate.opsForValue().get(STAGES_VERSION_KEY);
            Object updateTimeObj = redisTemplate.opsForValue().get(STAGES_UPDATE_TIME_KEY);

            List<GrowthStage> stages = getAllStages();

            if (versionObj != null) {
                versionDTO.setVersion(Long.valueOf(versionObj.toString()));
            }
            if (updateTimeObj != null) {
                versionDTO.setUpdateTime((LocalDateTime) updateTimeObj);
            }

            List<GrowthStageVersionDTO.StageItem> stageItems = stages.stream()
                    .map(s -> {
                        GrowthStageVersionDTO.StageItem item = new GrowthStageVersionDTO.StageItem();
                        item.setStageCode(s.getStageCode());
                        item.setStageName(s.getStageName());
                        item.setStageOrder(s.getStageOrder());
                        item.setDescription(s.getDescription());
                        return item;
                    })
                    .collect(Collectors.toList());

            versionDTO.setStages(stageItems);

            if (versionDTO.getVersion() == null) {
                versionDTO.setVersion(System.currentTimeMillis());
            }
            if (versionDTO.getUpdateTime() == null) {
                versionDTO.setUpdateTime(LocalDateTime.now());
            }

            return versionDTO;

        } catch (Exception e) {
            log.warn("获取带版本的生长阶段失败，将从数据库直接加载: {}", e.getMessage());
            List<GrowthStage> stages = growthStageRepository.findAllByOrderByStageOrderAsc();
            cacheStages(stages);

            List<GrowthStageVersionDTO.StageItem> stageItems = stages.stream()
                    .map(s -> {
                        GrowthStageVersionDTO.StageItem item = new GrowthStageVersionDTO.StageItem();
                        item.setStageCode(s.getStageCode());
                        item.setStageName(s.getStageName());
                        item.setStageOrder(s.getStageOrder());
                        item.setDescription(s.getDescription());
                        return item;
                    })
                    .collect(Collectors.toList());

            versionDTO.setVersion(System.currentTimeMillis());
            versionDTO.setUpdateTime(LocalDateTime.now());
            versionDTO.setStages(stageItems);
            return versionDTO;
        }
    }

    public void refreshStagesCache() {
        List<GrowthStage> stages = growthStageRepository.findAllByOrderByStageOrderAsc();
        cacheStages(stages);
    }

    private void cacheStages(List<GrowthStage> stages) {
        try {
            redisTemplate.delete(STAGES_CACHE_KEY);
            if (stages != null && !stages.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(STAGES_CACHE_KEY, stages.toArray());
                redisTemplate.expire(STAGES_CACHE_KEY, CACHE_TTL_HOURS, TimeUnit.HOURS);
            }

            long newVersion = System.currentTimeMillis();
            LocalDateTime updateTime = LocalDateTime.now();
            redisTemplate.opsForValue().set(STAGES_VERSION_KEY, newVersion, CACHE_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(STAGES_UPDATE_TIME_KEY, updateTime, CACHE_TTL_HOURS, TimeUnit.HOURS);

            log.info("生长阶段数据已缓存到Redis，共{}条，版本{}", stages != null ? stages.size() : 0, newVersion);
        } catch (Exception e) {
            log.error("缓存生长阶段数据失败: {}", e.getMessage());
        }
    }
}
