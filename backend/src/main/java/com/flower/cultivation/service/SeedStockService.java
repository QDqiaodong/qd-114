package com.flower.cultivation.service;

import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.repository.SeedInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedStockService {

    private final SeedInfoRepository seedInfoRepository;

    @Transactional
    public void deductForSowing(Long seedId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("扣减数量必须大于0");
        }
        SeedInfo seed = seedInfoRepository.findById(seedId)
                .orElseThrow(() -> new RuntimeException("种子信息不存在，ID: " + seedId));
        if (seed.getRemainingQuantity() < quantity) {
            throw new RuntimeException(
                    String.format("种子余量不足，品种：%s，剩余：%d，需要：%d",
                            seed.getVarietyName(), seed.getRemainingQuantity(), quantity));
        }
        seed.setRemainingQuantity(seed.getRemainingQuantity() - quantity);
        try {
            seedInfoRepository.save(seed);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("种子库存并发冲突，请稍后重试");
        }
        log.info("种子余量扣减：品种={}, 种子ID={}, 扣减={}, 扣减后余量={}",
                seed.getVarietyName(), seedId, quantity, seed.getRemainingQuantity());
    }

    @Transactional
    public void replenishOnSowingCancel(Long seedId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("回补数量必须大于0");
        }
        SeedInfo seed = seedInfoRepository.findById(seedId)
                .orElseThrow(() -> new RuntimeException("种子信息不存在，ID: " + seedId));
        seed.setRemainingQuantity(seed.getRemainingQuantity() + quantity);
        try {
            seedInfoRepository.save(seed);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new RuntimeException("种子库存并发冲突，请稍后重试");
        }
        log.info("种子余量回补：品种={}, 种子ID={}, 回补={}, 回补后余量={}",
                seed.getVarietyName(), seedId, quantity, seed.getRemainingQuantity());
    }

    @Transactional
    public void adjustForSowingUpdate(Long seedId, int oldQuantity, int newQuantity) {
        int diff = newQuantity - oldQuantity;
        if (diff == 0) {
            return;
        }
        if (diff > 0) {
            deductForSowing(seedId, diff);
        } else {
            replenishOnSowingCancel(seedId, -diff);
        }
    }

    @Transactional
    public void switchSeedForSowingUpdate(Long oldSeedId, int oldQuantity, Long newSeedId, int newQuantity) {
        replenishOnSowingCancel(oldSeedId, oldQuantity);
        deductForSowing(newSeedId, newQuantity);
    }
}
