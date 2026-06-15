package com.flower.cultivation.service;

import com.flower.cultivation.dto.SowingRecordResult;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SowingRecordService {

    private final SowingRecordRepository sowingRecordRepository;
    private final SeedInfoService seedInfoService;
    private final GrowthTrackingRepository growthTrackingRepository;
    private final TransplantRecordRepository transplantRecordRepository;

    public List<SowingRecord> findAll() {
        return sowingRecordRepository.findAllByOrderBySowingTimeDesc();
    }

    public SowingRecord findById(Long id) {
        return sowingRecordRepository.findById(id).orElse(null);
    }

    public List<SowingRecord> findBySeedId(Long seedId) {
        return sowingRecordRepository.findBySeedId(seedId);
    }

    public List<SowingRecord> findByVarietyId(Long varietyId) {
        return sowingRecordRepository.findByVarietyId(varietyId);
    }

    @Transactional
    public SowingRecordResult save(SowingRecord record) {
        if (record.getId() == null) {
            boolean success = seedInfoService.decreaseQuantity(record.getSeedId(), record.getSowingQuantity());
            if (!success) {
                throw new RuntimeException("播种数量不得超过种子剩余量");
            }
        } else {
            SowingRecord existing = findById(record.getId());
            if (existing == null) {
                throw new RuntimeException("播种记录不存在");
            }
            Long oldSeedId = existing.getSeedId();
            Integer oldQuantity = existing.getSowingQuantity();
            Long newSeedId = record.getSeedId();
            Integer newQuantity = record.getSowingQuantity();

            if (!oldSeedId.equals(newSeedId)) {
                seedInfoService.increaseQuantity(oldSeedId, oldQuantity);
                boolean success = seedInfoService.decreaseQuantity(newSeedId, newQuantity);
                if (!success) {
                    throw new RuntimeException("播种数量不得超过种子剩余量");
                }
            } else {
                int diff = newQuantity - oldQuantity;
                if (diff > 0) {
                    boolean success = seedInfoService.decreaseQuantity(newSeedId, diff);
                    if (!success) {
                        throw new RuntimeException("播种数量不得超过种子剩余量");
                    }
                } else if (diff < 0) {
                    seedInfoService.increaseQuantity(newSeedId, -diff);
                }
            }
        }
        SowingRecord savedRecord = sowingRecordRepository.save(record);
        SeedInfo seedInfo = seedInfoService.findById(savedRecord.getSeedId());
        Integer remainingQuantity = seedInfo != null ? seedInfo.getRemainingQuantity() : 0;
        return new SowingRecordResult(savedRecord, remainingQuantity);
    }

    @Transactional
    public void deleteById(Long id) {
        SowingRecord existing = findById(id);
        if (existing == null) {
            throw new RuntimeException("播种记录不存在");
        }
        if (growthTrackingRepository.existsBySowingId(id)) {
            throw new RuntimeException("该播种记录存在生长跟踪引用，无法删除");
        }
        if (transplantRecordRepository.existsBySowingId(id)) {
            throw new RuntimeException("该播种记录存在移栽记录引用，无法删除");
        }
        seedInfoService.increaseQuantity(existing.getSeedId(), existing.getSowingQuantity());
        sowingRecordRepository.deleteById(id);
    }
}
