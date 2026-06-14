package com.flower.cultivation.service;

import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.repository.FlowerVarietyRepository;
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
public class FlowerVarietyService {

    private final FlowerVarietyRepository flowerVarietyRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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
}
