package com.flower.cultivation.service;

import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.repository.SeedInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedInfoService {

    private final SeedInfoRepository seedInfoRepository;

    public List<SeedInfo> findAll() {
        return seedInfoRepository.findAllByOrderByCreateTimeDesc();
    }

    public SeedInfo findById(Long id) {
        return seedInfoRepository.findById(id).orElse(null);
    }

    public List<SeedInfo> findByVarietyId(Long varietyId) {
        return seedInfoRepository.findByVarietyId(varietyId);
    }

    @Transactional
    public SeedInfo save(SeedInfo seedInfo) {
        if (seedInfo.getId() == null && seedInfo.getRemainingQuantity() == null) {
            seedInfo.setRemainingQuantity(seedInfo.getInitialQuantity());
        }
        return seedInfoRepository.save(seedInfo);
    }

    @Transactional
    public void deleteById(Long id) {
        seedInfoRepository.deleteById(id);
    }

    @Transactional
    public boolean decreaseQuantity(Long id, int quantity) {
        SeedInfo seed = findById(id);
        if (seed != null && seed.getRemainingQuantity() >= quantity) {
            seed.setRemainingQuantity(seed.getRemainingQuantity() - quantity);
            seedInfoRepository.save(seed);
            return true;
        }
        return false;
    }
}
