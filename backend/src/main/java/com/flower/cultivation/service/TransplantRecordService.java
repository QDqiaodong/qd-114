package com.flower.cultivation.service;

import com.flower.cultivation.dto.BatchHealthDTO;
import com.flower.cultivation.dto.HealthAbnormalItemDTO;
import com.flower.cultivation.dto.TransplantDetailDTO;
import com.flower.cultivation.dto.TransplantRecoveryBoardDTO;
import com.flower.cultivation.dto.TransplantRecoveryItemDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransplantRecordService {

    private final TransplantRecordRepository transplantRecordRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;
    private final HealthStatusAggregationService healthStatusAggregationService;
    private final GrowthStageCacheService growthStageCacheService;

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
            pg.setStageName(growthStageCacheService.getStageName(lastBeforeTransplant.getStageCode(), lastBeforeTransplant.getStageName()));
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

    public TransplantRecoveryBoardDTO getRecoveryBoard() {
        List<TransplantRecord> allRecords = transplantRecordRepository.findAllByOrderByTransplantTimeDesc();
        List<GrowthTracking> allTrackings = growthTrackingRepository.findAll();

        Map<Long, GrowthTracking> latestTrackingBySowing = new HashMap<>();
        for (GrowthTracking t : allTrackings) {
            GrowthTracking existing = latestTrackingBySowing.get(t.getSowingId());
            if (existing == null || t.getRecordTime().isAfter(existing.getRecordTime())) {
                latestTrackingBySowing.put(t.getSowingId(), t);
            }
        }

        Set<String> healthyKeywords = new HashSet<>(Arrays.asList(
            "健康", "正常", "良好", "优秀", "佳", "健壮", "旺盛",
            "HEALTHY", "NORMAL", "GOOD", "EXCELLENT", "STRONG", "VIGOROUS"
        ));

        Map<String, Set<String>> abnormalTypeKeywords = new LinkedHashMap<>();
        Set<String> bingruo = new HashSet<>(Arrays.asList("病", "弱", "病害", "病弱", "病菌", "发病", "生病", "病态", "DISEASE", "SICK", "WEAK", "UNHEALTHY", "ILL"));
        abnormalTypeKeywords.put("病弱", bingruo);
        Set<String> weinian = new HashSet<>(Arrays.asList("萎蔫", "枯萎", "蔫", "蔫了", "打蔫", "垂", "萎", "WILT", "WILTING", "WITHER", "DROOP", "LIMP"));
        abnormalTypeKeywords.put("萎蔫", weinian);
        Set<String> fahuang = new HashSet<>(Arrays.asList("发黄", "黄化", "黄叶", "变黄", "叶黄", "YELLOW", "YELLOWING", "CHLOROSIS"));
        abnormalTypeKeywords.put("发黄", fahuang);
        Set<String> langen = new HashSet<>(Arrays.asList("烂根", "根腐", "根烂", "根系差", "根系弱", "ROOT_ROT", "ROT", "BAD_ROOT", "POOR_ROOT"));
        abnormalTypeKeywords.put("烂根", langen);
        Set<String> chonghai = new HashSet<>(Arrays.asList("虫害", "虫", "蚜虫", "红蜘蛛", "介壳虫", "害虫", "PEST", "INSECT", "BUG", "APHID"));
        abnormalTypeKeywords.put("虫害", chonghai);

        LocalDate today = LocalDate.now();
        List<TransplantRecoveryItemDTO> allItems = new ArrayList<>();
        List<TransplantRecoveryItemDTO> needObservationItems = new ArrayList<>();

        int observingCount = 0;
        int recoveredCount = 0;
        int abnormalCount = 0;

        Map<Long, TransplantRecoveryBoardDTO.RecoveryStatByVariety> varietyMap = new LinkedHashMap<>();

        for (TransplantRecord record : allRecords) {
            TransplantRecoveryItemDTO item = new TransplantRecoveryItemDTO();
            item.setId(record.getId());
            item.setSowingId(record.getSowingId());
            item.setVarietyId(record.getVarietyId());
            item.setVarietyName(record.getVarietyName());
            item.setTransplantTime(record.getTransplantTime());
            item.setPotSpecification(record.getPotSpecification());
            item.setRecoveryTips(record.getRecoveryTips());
            item.setLightRequirement(record.getLightRequirement());
            item.setWateringFrequency(record.getWateringFrequency());
            item.setTransplantQuantity(record.getTransplantQuantity());

            long daysSince = 0;
            if (record.getTransplantTime() != null) {
                daysSince = ChronoUnit.DAYS.between(record.getTransplantTime().toLocalDate(), today);
            }
            item.setDaysSinceTransplant(daysSince);

            GrowthTracking latestTracking = latestTrackingBySowing.get(record.getSowingId());
            if (latestTracking != null) {
                item.setLatestHealthStatus(latestTracking.getHealthStatus());
                item.setLatestTrackingTime(latestTracking.getRecordTime());
                item.setCurrentStageName(growthStageCacheService.getStageName(latestTracking.getStageCode(), latestTracking.getStageName()));
            }

            String healthStatus = item.getLatestHealthStatus();
            boolean isHealthy = false;
            boolean isAbnormal = false;
            String abnormalType = null;

            if (healthStatus != null && !healthStatus.trim().isEmpty()) {
                String upperStatus = healthStatus.toUpperCase();
                String lowerStatus = healthStatus.toLowerCase();

                for (String kw : healthyKeywords) {
                    if (healthStatus.contains(kw) || upperStatus.contains(kw) || lowerStatus.contains(kw.toLowerCase())) {
                        isHealthy = true;
                        break;
                    }
                }

                if (!isHealthy) {
                    for (Map.Entry<String, Set<String>> entry : abnormalTypeKeywords.entrySet()) {
                        boolean found = false;
                        for (String kw : entry.getValue()) {
                            if (healthStatus.contains(kw) || upperStatus.contains(kw) || lowerStatus.contains(kw.toLowerCase())) {
                                isAbnormal = true;
                                abnormalType = entry.getKey();
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                }
            }

            item.setHasAbnormal(isAbnormal);
            item.setLatestAbnormalType(abnormalType);

            String recoveryStatus;
            String recoveryStatusText;
            String recoveryStatusLevel;
            boolean needObservation = false;
            String observationReason = "";

            if (isAbnormal) {
                recoveryStatus = "ABNORMAL";
                recoveryStatusText = "需关注";
                recoveryStatusLevel = "DANGER";
                needObservation = true;
                observationReason = "存在健康异常：" + (abnormalType != null ? abnormalType : healthStatus);
                abnormalCount++;
            } else if (daysSince <= 7) {
                recoveryStatus = "ACCLIMATING";
                recoveryStatusText = "缓苗期";
                recoveryStatusLevel = "WARNING";
                needObservation = true;
                observationReason = "移栽后" + daysSince + "天，处于缓苗期";
                observingCount++;
            } else if (daysSince <= 14) {
                recoveryStatus = "RECOVERING";
                recoveryStatusText = "恢复中";
                recoveryStatusLevel = "INFO";
                needObservation = true;
                observationReason = "移栽后" + daysSince + "天，恢复观察期";
                observingCount++;
            } else {
                recoveryStatus = "RECOVERED";
                recoveryStatusText = "已恢复";
                recoveryStatusLevel = "SUCCESS";
                recoveredCount++;
            }

            if (!isAbnormal && latestTracking == null && daysSince <= 14) {
                needObservation = true;
                if (!observationReason.isEmpty()) {
                    observationReason += "；暂无生长跟踪记录";
                } else {
                    observationReason = "暂无生长跟踪记录，建议观察";
                }
            }

            item.setRecoveryStatus(recoveryStatus);
            item.setRecoveryStatusText(recoveryStatusText);
            item.setRecoveryStatusLevel(recoveryStatusLevel);
            item.setNeedObservation(needObservation);
            item.setObservationReason(observationReason);

            allItems.add(item);

            if (needObservation) {
                needObservationItems.add(item);
            }

            Long varietyId = record.getVarietyId();
            if (varietyId != null) {
                TransplantRecoveryBoardDTO.RecoveryStatByVariety stat = varietyMap.computeIfAbsent(varietyId, k -> {
                    TransplantRecoveryBoardDTO.RecoveryStatByVariety s = new TransplantRecoveryBoardDTO.RecoveryStatByVariety();
                    s.setVarietyId(varietyId);
                    s.setVarietyName(record.getVarietyName());
                    s.setTotalCount(0);
                    s.setObservingCount(0);
                    s.setAbnormalCount(0);
                    return s;
                });
                stat.setTotalCount(stat.getTotalCount() + 1);
                if (needObservation && !isAbnormal) {
                    stat.setObservingCount(stat.getObservingCount() + 1);
                }
                if (isAbnormal) {
                    stat.setAbnormalCount(stat.getAbnormalCount() + 1);
                }
            }
        }

        needObservationItems.sort((a, b) -> {
            int levelOrderA = getLevelOrder(a.getRecoveryStatusLevel());
            int levelOrderB = getLevelOrder(b.getRecoveryStatusLevel());
            if (levelOrderA != levelOrderB) {
                return levelOrderA - levelOrderB;
            }
            if (a.getTransplantTime() != null && b.getTransplantTime() != null) {
                return a.getTransplantTime().compareTo(b.getTransplantTime());
            }
            return 0;
        });

        TransplantRecoveryBoardDTO result = new TransplantRecoveryBoardDTO();
        result.setTotalCount(allItems.size());
        result.setObservingCount(observingCount);
        result.setRecoveredCount(recoveredCount);
        result.setAbnormalCount(abnormalCount);
        result.setNeedObservationItems(needObservationItems);
        result.setAllItems(allItems);
        result.setByVariety(new ArrayList<>(varietyMap.values()));

        return result;
    }

    private int getLevelOrder(String level) {
        if (level == null) return 99;
        switch (level) {
            case "DANGER": return 0;
            case "WARNING": return 1;
            case "INFO": return 2;
            case "SUCCESS": return 3;
            default: return 99;
        }
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

        List<GrowthTracking> allTrackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(record.getSowingId());
        if (allTrackings.isEmpty()) {
            throw new RuntimeException("该播种批次尚无生长跟踪记录，未达到移栽条件");
        }

        GrowthTracking latestTrackingAll = allTrackings.get(allTrackings.size() - 1);
        int latestStageOrder = growthStageCacheService.getAllStages().stream()
                .filter(s -> s.getStageCode().equals(latestTrackingAll.getStageCode()))
                .map(st -> st.getStageOrder())
                .findFirst()
                .orElse(0);

        int seedlingStageOrder = growthStageCacheService.getAllStages().stream()
                .filter(s -> "SEEDLING".equals(s.getStageCode()))
                .map(st -> st.getStageOrder())
                .findFirst()
                .orElse(5);

        if (latestStageOrder < seedlingStageOrder) {
            String latestStageName = growthStageCacheService.getStageName(latestTrackingAll.getStageCode(), latestTrackingAll.getStageName());
            throw new RuntimeException(
                    String.format("该播种批次当前处于【%s】阶段，尚未达到移栽条件，需至少进入【成苗期】", latestStageName));
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

        GrowthTracking latestTracking = allTrackings.stream()
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
