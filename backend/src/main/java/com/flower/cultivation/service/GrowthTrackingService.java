package com.flower.cultivation.service;

import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrowthTrackingService {

    private final GrowthTrackingRepository growthTrackingRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthStageCacheService growthStageCacheService;

    public List<GrowthTracking> findBySowingId(Long sowingId) {
        return growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowingId);
    }

    public GrowthTracking findById(Long id) {
        return growthTrackingRepository.findById(id).orElse(null);
    }

    @Transactional
    public GrowthTracking save(GrowthTracking tracking) {
        validateGrowthTracking(tracking);
        return growthTrackingRepository.save(tracking);
    }

    @Transactional
    public void deleteById(Long id) {
        growthTrackingRepository.deleteById(id);
    }

    public List<GrowthTracking> findBySowingIdAndStageCode(Long sowingId, String stageCode) {
        return growthTrackingRepository.findBySowingIdAndStageCode(sowingId, stageCode);
    }

    private void validateGrowthTracking(GrowthTracking tracking) {
        SowingRecord sowing = sowingRecordRepository.findById(tracking.getSowingId()).orElse(null);
        if (sowing == null) {
            throw new RuntimeException("播种记录不存在");
        }

        if (tracking.getRecordTime() != null && tracking.getRecordTime().isBefore(sowing.getSowingTime())) {
            throw new RuntimeException("记录时间不能早于播种时间");
        }

        if (tracking.getRecordTime() != null && tracking.getRecordTime().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("记录时间不能是未来时间");
        }

        Optional<GrowthStage> currentStageOpt = growthStageCacheService.getAllStages().stream()
                .filter(s -> s.getStageCode().equals(tracking.getStageCode()))
                .findFirst();

        if (currentStageOpt.isEmpty()) {
            throw new RuntimeException("生长阶段不存在: " + tracking.getStageCode());
        }

        int currentStageOrder = currentStageOpt.get().getStageOrder();

        List<GrowthTracking> existingTrackings;
        if (tracking.getId() == null) {
            existingTrackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(tracking.getSowingId());
        } else {
            existingTrackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(tracking.getSowingId())
                    .stream()
                    .filter(t -> !t.getId().equals(tracking.getId()))
                    .toList();
        }

        if (!existingTrackings.isEmpty()) {
            Optional<Integer> maxStageOrderOpt = existingTrackings.stream()
                    .map(t -> {
                        return growthStageCacheService.getAllStages().stream()
                                .filter(s -> s.getStageCode().equals(t.getStageCode()))
                                .map(GrowthStage::getStageOrder)
                                .findFirst()
                                .orElse(0);
                    })
                    .max(Comparator.naturalOrder());

            if (maxStageOrderOpt.isPresent() && currentStageOrder < maxStageOrderOpt.get()) {
                String maxStageName = growthStageCacheService.getAllStages().stream()
                        .filter(s -> s.getStageOrder().equals(maxStageOrderOpt.get()))
                        .map(GrowthStage::getStageName)
                        .findFirst()
                        .orElse("未知阶段");
                throw new RuntimeException(
                        String.format("生长阶段不能倒序，当前已记录最高阶段为【%s】", maxStageName)
                );
            }

            Optional<LocalDateTime> maxRecordTimeOpt = existingTrackings.stream()
                    .map(GrowthTracking::getRecordTime)
                    .max(Comparator.naturalOrder());

            if (tracking.getRecordTime() != null && maxRecordTimeOpt.isPresent()
                    && tracking.getRecordTime().isBefore(maxRecordTimeOpt.get())) {
                throw new RuntimeException("记录时间不能早于已有记录的最新时间");
            }
        }
    }
}
