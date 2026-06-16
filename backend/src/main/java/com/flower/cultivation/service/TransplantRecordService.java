package com.flower.cultivation.service;

import com.flower.cultivation.dto.BatchHealthDTO;
import com.flower.cultivation.dto.HealthAbnormalItemDTO;
import com.flower.cultivation.dto.TransplantDetailDTO;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransplantRecordService {

    private final TransplantRecordRepository transplantRecordRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;
    private final HealthStatusAggregationService healthStatusAggregationService;

    public List<TransplantRecord> findAll() {
        return transplantRecordRepository.findAllByOrderByTransplantTimeDesc();
    }

    public TransplantRecord findById(Long id) {
        return transplantRecordRepository.findById(id).orElse(null);
    }

    public List<TransplantRecord> findBySowingId(Long sowingId) {
        return transplantRecordRepository.findBySowingId(sowingId);
    }

    public TransplantDetailDTO findDetailById(Long id) {
        TransplantRecord record = transplantRecordRepository.findById(id).orElse(null);
        if (record == null) {
            return null;
        }

        TransplantDetailDTO dto = new TransplantDetailDTO();
        dto.setId(record.getId());
        dto.setSowingId(record.getSowingId());
        dto.setVarietyId(record.getVarietyId());
        dto.setVarietyName(record.getVarietyName());
        dto.setTransplantTime(record.getTransplantTime());
        dto.setPotSpecification(record.getPotSpecification());
        dto.setSoilRatio(record.getSoilRatio());
        dto.setTransplantQuantity(record.getTransplantQuantity());
        dto.setCumulativeQuantity(record.getCumulativeQuantity());
        dto.setRecoveryTips(record.getRecoveryTips());
        dto.setLightRequirement(record.getLightRequirement());
        dto.setWateringFrequency(record.getWateringFrequency());
        dto.setFertilizationPlan(record.getFertilizationPlan());
        dto.setNotes(record.getNotes());

        SowingRecord sowing = sowingRecordRepository.findById(record.getSowingId()).orElse(null);
        if (sowing != null) {
            dto.setSowingQuantity(sowing.getSowingQuantity());
        }

        List<TransplantRecord> sameSowingRecords = transplantRecordRepository.findBySowingId(record.getSowingId());
        int previousCumulative = sameSowingRecords.stream()
                .filter(r -> r.getTransplantTime() != null && record.getTransplantTime() != null
                        && r.getTransplantTime().isBefore(record.getTransplantTime()))
                .mapToInt(TransplantRecord::getTransplantQuantity)
                .sum();
        dto.setPreviousCumulativeQuantity(previousCumulative);

        List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(record.getSowingId());
        GrowthTracking lastBeforeTransplant = trackings.stream()
                .filter(t -> t.getRecordTime() != null && record.getTransplantTime() != null
                        && !t.getRecordTime().isAfter(record.getTransplantTime()))
                .max(Comparator.comparing(GrowthTracking::getRecordTime))
                .orElse(null);

        if (lastBeforeTransplant != null) {
            TransplantDetailDTO.PreTransplantGrowth pg = new TransplantDetailDTO.PreTransplantGrowth();
            pg.setId(lastBeforeTransplant.getId());
            pg.setStageCode(lastBeforeTransplant.getStageCode());
            pg.setStageName(lastBeforeTransplant.getStageName());
            pg.setRecordTime(lastBeforeTransplant.getRecordTime());
            pg.setPlantHeight(lastBeforeTransplant.getPlantHeight());
            pg.setLeafCount(lastBeforeTransplant.getLeafCount());
            pg.setRootDevelopment(lastBeforeTransplant.getRootDevelopment());
            pg.setHealthStatus(lastBeforeTransplant.getHealthStatus());
            pg.setTemperature(lastBeforeTransplant.getTemperature());
            pg.setHumidity(lastBeforeTransplant.getHumidity());
            pg.setLightHours(lastBeforeTransplant.getLightHours());
            pg.setFertilization(lastBeforeTransplant.getFertilization());
            pg.setEstimatedSurvival(lastBeforeTransplant.getEstimatedSurvival());
            pg.setNotes(lastBeforeTransplant.getNotes());
            dto.setLastGrowthBeforeTransplant(pg);
            dto.setEstimatedSurvival(lastBeforeTransplant.getEstimatedSurvival());
        }

        dto.setHealthAssessment(buildHealthAssessment(record.getSowingId()));

        return dto;
    }

    private TransplantDetailDTO.TransplantHealthAssessment buildHealthAssessment(Long sowingId) {
        TransplantDetailDTO.TransplantHealthAssessment assessment =
                new TransplantDetailDTO.TransplantHealthAssessment();

        try {
            BatchHealthDTO batchHealth = healthStatusAggregationService.getBatchHealth(sowingId);
            if (batchHealth == null) {
                assessment.setHasAbnormal(false);
                assessment.setAbnormalRecordCount(0);
                assessment.setTotalRecordCount(0);
                assessment.setAbnormalRate(0.0);
                assessment.setTransplantRiskLevel("LOW");
                assessment.setRiskDescription("暂无健康记录数据");
                assessment.setRecoveryAdvice("按正常移栽流程操作，注意缓苗养护");
                return assessment;
            }

            assessment.setHasAbnormal(batchHealth.getHasAbnormal());
            assessment.setAbnormalRecordCount(batchHealth.getAbnormalCount());
            assessment.setTotalRecordCount(batchHealth.getTrackingCount());
            assessment.setLatestHealthStatus(batchHealth.getLatestHealthStatus());

            double abnormalRate = batchHealth.getTrackingCount() > 0
                    ? (batchHealth.getAbnormalCount() * 100.0 / batchHealth.getTrackingCount())
                    : 0.0;
            assessment.setAbnormalRate(abnormalRate);

            String riskLevel;
            String riskDescription;
            String recoveryAdvice;

            if (!Boolean.TRUE.equals(batchHealth.getHasAbnormal())) {
                riskLevel = "LOW";
                riskDescription = "植株健康状况良好，适合移栽";
                recoveryAdvice = "按正常移栽流程操作，移栽后注意缓苗一周，避免强光直射";
            } else if (abnormalRate <= 30) {
                riskLevel = "MEDIUM";
                riskDescription = "存在轻度健康异常，建议谨慎移栽";
                recoveryAdvice = "建议先改善养护条件，待植株恢复健康后再移栽。移栽时挑选健康壮苗，淘汰病弱株。移栽后加强养护，密切关注生长状态";
            } else {
                riskLevel = "HIGH";
                riskDescription = "健康问题较严重，不建议当前移栽";
                recoveryAdvice = "建议先进行针对性治疗，待植株恢复健康后再考虑移栽。如需紧急移栽，务必挑选最健康的个体，且移栽后需特别加强护理";
            }

            assessment.setTransplantRiskLevel(riskLevel);
            assessment.setRiskDescription(riskDescription);
            assessment.setRecoveryAdvice(recoveryAdvice);

            List<HealthAbnormalItemDTO> allAbnormal = healthStatusAggregationService.getAggregation().getAbnormalDetails();
            allAbnormal.stream()
                    .filter(a -> a.getSowingId().equals(sowingId))
                    .findFirst()
                    .ifPresent(a -> {
                        assessment.setLatestAbnormalType(a.getAbnormalType());
                        assessment.setLatestSeverityLevel(a.getSeverityLevel());
                    });

        } catch (Exception e) {
            log.warn("构建移栽健康评估失败: {}", e.getMessage());
            assessment.setHasAbnormal(false);
            assessment.setTransplantRiskLevel("UNKNOWN");
            assessment.setRiskDescription("健康评估数据加载失败");
            assessment.setRecoveryAdvice("请参考生长记录进行人工评估");
        }

        return assessment;
    }

    @Transactional
    public TransplantRecord save(TransplantRecord record) {
        validateTransplantRecord(record);
        calculateCumulativeQuantity(record);
        return transplantRecordRepository.save(record);
    }

    @Transactional
    public void deleteById(Long id) {
        transplantRecordRepository.deleteById(id);
    }

    private void calculateCumulativeQuantity(TransplantRecord record) {
        List<TransplantRecord> existingRecords;
        if (record.getId() == null) {
            existingRecords = transplantRecordRepository.findBySowingId(record.getSowingId());
        } else {
            existingRecords = transplantRecordRepository.findBySowingIdAndIdNot(record.getSowingId(), record.getId());
        }

        int previousTotal = existingRecords.stream()
                .mapToInt(TransplantRecord::getTransplantQuantity)
                .sum();

        record.setCumulativeQuantity(previousTotal + record.getTransplantQuantity());
    }

    private void validateTransplantRecord(TransplantRecord record) {
        SowingRecord sowing = sowingRecordRepository.findById(record.getSowingId()).orElse(null);
        if (sowing == null) {
            throw new RuntimeException("播种记录不存在");
        }

        if (record.getTransplantTime() != null && record.getTransplantTime().isBefore(sowing.getSowingTime())) {
            throw new RuntimeException("移栽时间不能早于播种时间");
        }

        List<TransplantRecord> existingRecords;
        if (record.getId() == null) {
            existingRecords = transplantRecordRepository.findBySowingId(record.getSowingId());
        } else {
            existingRecords = transplantRecordRepository.findBySowingIdAndIdNot(record.getSowingId(), record.getId());
        }

        int totalTransplanted = existingRecords.stream()
                .mapToInt(TransplantRecord::getTransplantQuantity)
                .sum();

        if (record.getTransplantQuantity() == null || record.getTransplantQuantity() <= 0) {
            throw new RuntimeException("移栽数量必须大于0");
        }

        int maxAllowed = sowing.getSowingQuantity();

        List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(record.getSowingId());
        GrowthTracking latestTracking = trackings.stream()
                .filter(t -> t.getRecordTime() != null && record.getTransplantTime() != null
                        && !t.getRecordTime().isAfter(record.getTransplantTime()))
                .max(Comparator.comparing(GrowthTracking::getRecordTime))
                .orElse(null);

        if (latestTracking != null && latestTracking.getEstimatedSurvival() != null
                && latestTracking.getEstimatedSurvival() > 0) {
            maxAllowed = Math.min(maxAllowed, latestTracking.getEstimatedSurvival());
        }

        if (totalTransplanted + record.getTransplantQuantity() > maxAllowed) {
            if (maxAllowed < sowing.getSowingQuantity()) {
                throw new RuntimeException(
                        String.format("移栽总数(%d)不得超过存活估算数量(%d)（播种数量%d），当前已移栽%d棵",
                                totalTransplanted + record.getTransplantQuantity(),
                                maxAllowed,
                                sowing.getSowingQuantity(),
                                totalTransplanted)
                );
            } else {
                throw new RuntimeException(
                        String.format("移栽总数(%d)不得超过播种数量(%d)，当前已移栽%d棵",
                                totalTransplanted + record.getTransplantQuantity(),
                                sowing.getSowingQuantity(),
                                totalTransplanted)
                );
            }
        }
    }
}
