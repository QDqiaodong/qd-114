package com.flower.cultivation.service;

import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.repository.GrowthStageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthStageCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GrowthStageRepository growthStageRepository;

    private static final String STAGES_CACHE_KEY = "growth:stages:list";
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
            log.info("生长阶段数据已缓存到Redis，共{}条", stages.size());
        } catch (Exception e) {
            log.error("缓存生长阶段数据失败: {}", e.getMessage());
        }
    }
}
