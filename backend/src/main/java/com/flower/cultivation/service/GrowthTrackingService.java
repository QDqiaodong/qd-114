package com.flower.cultivation.service;

import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.exception.StageTransitionException;
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
        List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowingId);
        trackings.forEach(this::enrichStageName);
        return trackings;
    }

    public GrowthTracking findById(Long id) {
        GrowthTracking tracking = growthTrackingRepository.findById(id).orElse(null);
        if (tracking != null) {
            enrichStageName(tracking);
        }
        return tracking;
    }

    public List<GrowthTracking> findBySowingIdAndStageCode(Long sowingId, String stageCode) {
        List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdAndStageCode(sowingId, stageCode);
        trackings.forEach(this::enrichStageName);
        return trackings;
    }

    private void enrichStageName(GrowthTracking tracking) {
        if (tracking == null) {
            return;
        }
        String resolvedName = growthStageCacheService.getStageName(tracking.getStageCode(), tracking.getStageName());
        tracking.setStageName(resolvedName);
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

        List<GrowthStage> allStages = growthStageCacheService.getAllStages();
        Optional<GrowthStage> targetStageOpt = allStages.stream()
                .filter(s -> s.getStageCode().equals(tracking.getStageCode()))
                .findFirst();

        if (targetStageOpt.isEmpty()) {
            throw new RuntimeException("生长阶段不存在: " + tracking.getStageCode());
        }

        GrowthStage targetStage = targetStageOpt.get();
        int targetOrder = targetStage.getStageOrder();
        String targetName = targetStage.getStageName();

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
            Optional<GrowthStage> maxStageOpt = existingTrackings.stream()
                    .map(t -> allStages.stream()
                            .filter(s -> s.getStageCode().equals(t.getStageCode()))
                            .findFirst()
                            .orElse(null))
                    .filter(s -> s != null)
                    .max(Comparator.comparingInt(GrowthStage::getStageOrder));

            if (maxStageOpt.isPresent()) {
                GrowthStage maxStage = maxStageOpt.get();
                int maxOrder = maxStage.getStageOrder();
                String maxName = maxStage.getStageName();

                boolean isDuplicate = existingTrackings.stream()
                        .anyMatch(t -> t.getStageCode().equals(tracking.getStageCode()));

                if (isDuplicate) {
                    throw new StageTransitionException(
                            String.format("生长阶段不可重复记录，该播种记录已记录过【%s】阶段", targetName),
                            StageTransitionException.TransitionType.DUPLICATE,
                            maxName,
                            targetName
                    );
                }

                if (targetOrder < maxOrder) {
                    throw new StageTransitionException(
                            String.format("生长阶段不能倒退，当前最高阶段为【%s】，无法回退至【%s】", maxName, targetName),
                            StageTransitionException.TransitionType.BACKWARD,
                            maxName,
                            targetName
                    );
                }

                if (targetOrder > maxOrder + 1) {
                    String expectedStageName = allStages.stream()
                            .filter(s -> s.getStageOrder() == maxOrder + 1)
                            .map(GrowthStage::getStageName)
                            .findFirst()
                            .orElse("未知阶段");
                    throw new StageTransitionException(
                            String.format("生长阶段不能跳级，当前最高阶段为【%s】，下一阶段应为【%s】，无法直接跳至【%s】",
                                    maxName, expectedStageName, targetName),
                            StageTransitionException.TransitionType.SKIP,
                            maxName,
                            targetName
                    );
                }
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
