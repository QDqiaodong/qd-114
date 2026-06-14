package com.flower.cultivation.service;

import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SowingRecordService {

    private final SowingRecordRepository sowingRecordRepository;
    private final SeedInfoService seedInfoService;

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
    public SowingRecord save(SowingRecord record) {
        if (record.getId() == null) {
            boolean success = seedInfoService.decreaseQuantity(record.getSeedId(), record.getSowingQuantity());
            if (!success) {
                throw new RuntimeException("种子数量不足");
            }
        }
        return sowingRecordRepository.save(record);
    }

    @Transactional
    public void deleteById(Long id) {
        sowingRecordRepository.deleteById(id);
    }
}
