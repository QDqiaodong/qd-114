package com.flower.cultivation.service;

import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GrowthTrackingService {

    private final GrowthTrackingRepository growthTrackingRepository;

    public List<GrowthTracking> findBySowingId(Long sowingId) {
        return growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowingId);
    }

    public GrowthTracking findById(Long id) {
        return growthTrackingRepository.findById(id).orElse(null);
    }

    @Transactional
    public GrowthTracking save(GrowthTracking tracking) {
        return growthTrackingRepository.save(tracking);
    }

    @Transactional
    public void deleteById(Long id) {
        growthTrackingRepository.deleteById(id);
    }

    public List<GrowthTracking> findBySowingIdAndStageCode(Long sowingId, String stageCode) {
        return growthTrackingRepository.findBySowingIdAndStageCode(sowingId, stageCode);
    }
}
