package com.flower.cultivation.service;

import com.flower.cultivation.dto.BatchHealthDTO;
import com.flower.cultivation.dto.HealthAbnormalItemDTO;
import com.flower.cultivation.dto.HealthStatusAggregationDTO;
import com.flower.cultivation.dto.StageHealthDTO;
import com.flower.cultivation.dto.VarietyHealthDTO;
import com.flower.cultivation.entity.GrowthStage;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthStatusAggregationService {

    private final GrowthTrackingRepository growthTrackingRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthStageCacheService growthStageCacheService;

    private static final Set<String> HEALTHY_KEYWORDS = new HashSet<>(Arrays.asList(
            "健康", "正常", "良好", "优秀", "佳", "健壮", "旺盛",
            "HEALTHY", "NORMAL", "GOOD", "EXCELLENT", "STRONG", "VIGOROUS"
    ));

    private static final Map<String, Set<String>> ABNORMAL_TYPE_KEYWORDS;

    static {
        Map<String, Set<String>> map = new LinkedHashMap<>();

        Set<String> bingruo = new HashSet<>();
        bingruo.add("病");
        bingruo.add("弱");
        bingruo.add("病害");
        bingruo.add("病弱");
        bingruo.add("病菌");
        bingruo.add("发病");
        bingruo.add("生病");
        bingruo.add("病态");
        bingruo.add("DISEASE");
        bingruo.add("SICK");
        bingruo.add("WEAK");
        bingruo.add("UNHEALTHY");
        bingruo.add("ILL");
        map.put("病弱", bingruo);

        Set<String> tuzhang = new HashSet<>();
        tuzhang.add("徒长");
        tuzhang.add("陡长");
        tuzhang.add("细长");
        tuzhang.add("高脚");
        tuzhang.add("徒");
        tuzhang.add("SPINDLY");
        tuzhang.add("LEGGY");
        tuzhang.add("ETIOLATION");
        tuzhang.add("STRETCHED");
        map.put("徒长", tuzhang);

        Set<String> weinian = new HashSet<>();
        weinian.add("萎蔫");
        weinian.add("枯萎");
        weinian.add("蔫");
        weinian.add("蔫了");
        weinian.add("打蔫");
        weinian.add("垂");
        weinian.add("萎");
        weinian.add("WILT");
        weinian.add("WILTING");
        weinian.add("WITHER");
        weinian.add("DROOP");
        weinian.add("LIMP");
        map.put("萎蔫", weinian);

        Set<String> fahuang = new HashSet<>();
        fahuang.add("发黄");
        fahuang.add("黄化");
        fahuang.add("黄叶");
        fahuang.add("变黄");
        fahuang.add("叶黄");
        fahuang.add("YELLOW");
        fahuang.add("YELLOWING");
        fahuang.add("CHLOROSIS");
        map.put("发黄", fahuang);

        Set<String> langen = new HashSet<>();
        langen.add("烂根");
        langen.add("根腐");
        langen.add("根烂");
        langen.add("根系差");
        langen.add("根系弱");
        langen.add("ROOT_ROT");
        langen.add("ROT");
        langen.add("BAD_ROOT");
        langen.add("POOR_ROOT");
        map.put("烂根", langen);

        Set<String> chonghai = new HashSet<>();
        chonghai.add("虫害");
        chonghai.add("虫");
        chonghai.add("蚜虫");
        chonghai.add("红蜘蛛");
        chonghai.add("介壳虫");
        chonghai.add("害虫");
        chonghai.add("PEST");
        chonghai.add("INSECT");
        chonghai.add("BUG");
        chonghai.add("APHID");
        map.put("虫害", chonghai);

        Set<String> quesu = new HashSet<>();
        quesu.add("缺素");
        quesu.add("缺肥");
        quesu.add("营养不良");
        quesu.add("脱肥");
        quesu.add("NUTRIENT_DEFICIENCY");
        quesu.add("DEFICIENCY");
        quesu.add("MALNUTRITION");
        map.put("缺素", quesu);

        Set<String> yaohai = new HashSet<>();
        yaohai.add("药害");
        yaohai.add("肥害");
        yaohai.add("灼伤");
        yaohai.add("烧苗");
        yaohai.add("PHYTOTOXICITY");
        yaohai.add("BURN");
        yaohai.add("FERTILIZER_BURN");
        map.put("药害", yaohai);

        ABNORMAL_TYPE_KEYWORDS = java.util.Collections.unmodifiableMap(map);
    }

    private static final Set<String> NEUTRAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "-", "/", "一般", "普通", "GENERAL", "AVERAGE", "FAIR", "OK"
    ));

    public HealthStatusAggregationDTO getAggregation() {
        List<GrowthTracking> allTrackings = growthTrackingRepository.findAll();
        List<SowingRecord> allSowings = sowingRecordRepository.findAll();
        List<GrowthStage> allStages = growthStageCacheService.getAllStages();

        Map<Long, SowingRecord> sowingMap = allSowings.stream()
                .collect(Collectors.toMap(SowingRecord::getId, s -> s));

        Map<String, GrowthStage> stageMap = allStages.stream()
                .collect(Collectors.toMap(GrowthStage::getStageCode, s -> s));

        HealthStatusAggregationDTO result = new HealthStatusAggregationDTO();

        int totalCount = allTrackings.size();
        int abnormalCount = 0;
        int normalCount = 0;
        int unknownCount = 0;

        List<HealthAbnormalItemDTO> abnormalDetails = new ArrayList<>();

        Map<Long, VarietyAccumulator> varietyMap = new LinkedHashMap<>();
        Map<Long, BatchAccumulator> batchMap = new LinkedHashMap<>();
        Map<String, StageAccumulator> stageMapAcc = new LinkedHashMap<>();

        for (GrowthStage stage : allStages) {
            StageAccumulator sa = new StageAccumulator();
            sa.stageCode = stage.getStageCode();
            sa.stageName = stage.getStageName();
            sa.stageOrder = stage.getStageOrder();
            stageMapAcc.put(stage.getStageCode(), sa);
        }

        for (GrowthTracking tracking : allTrackings) {
            String healthStatus = tracking.getHealthStatus();
            String abnormalType = null;
            boolean isAbnormal = false;
            boolean isNormal = false;

            if (healthStatus == null || healthStatus.trim().isEmpty()) {
                unknownCount++;
            } else {
                String upperStatus = healthStatus.toUpperCase();
                String lowerStatus = healthStatus.toLowerCase();

                for (String kw : HEALTHY_KEYWORDS) {
                    if (healthStatus.contains(kw) || upperStatus.contains(kw) || lowerStatus.contains(kw.toLowerCase())) {
                        isNormal = true;
                        break;
                    }
                }

                if (!isNormal) {
                    for (Map.Entry<String, Set<String>> entry : ABNORMAL_TYPE_KEYWORDS.entrySet()) {
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

                if (!isNormal && !isAbnormal) {
                    boolean isNeutral = false;
                    for (String kw : NEUTRAL_KEYWORDS) {
                        if (healthStatus.equals(kw) || upperStatus.equals(kw)) {
                            isNeutral = true;
                            break;
                        }
                    }
                    if (isNeutral) {
                        unknownCount++;
                    } else {
                        isAbnormal = true;
                        abnormalType = "其他异常";
                    }
                }
            }

            if (isNormal) normalCount++;
            if (isAbnormal) abnormalCount++;

            SowingRecord sowing = sowingMap.get(tracking.getSowingId());
            Long varietyId = sowing != null ? sowing.getVarietyId() : null;
            String varietyName = sowing != null ? sowing.getVarietyName() : "未知品种";

            if (isAbnormal) {
                HealthAbnormalItemDTO item = new HealthAbnormalItemDTO();
                item.setTrackingId(tracking.getId());
                item.setSowingId(tracking.getSowingId());
                item.setVarietyId(varietyId);
                item.setVarietyName(varietyName);
                item.setStageCode(tracking.getStageCode());
                item.setStageName(tracking.getStageName());
                item.setHealthStatus(healthStatus);
                item.setAbnormalType(abnormalType);
                item.setSeverityLevel(determineSeverity(healthStatus, abnormalType));
                item.setRecordTime(tracking.getRecordTime());
                item.setEstimatedSurvival(tracking.getEstimatedSurvival());
                abnormalDetails.add(item);
            }

            if (varietyId != null) {
                VarietyAccumulator va = varietyMap.computeIfAbsent(varietyId, k -> {
                    VarietyAccumulator v = new VarietyAccumulator();
                    v.varietyId = varietyId;
                    v.varietyName = varietyName;
                    return v;
                });
                va.trackingCount++;
                if (isAbnormal) {
                    va.abnormalCount++;
                    va.abnormalTypes.add(abnormalType);
                }
                if (isNormal) va.normalCount++;

                String stageCode = tracking.getStageCode();
                VarietyStageAccumulator vsa = va.stageMap.computeIfAbsent(stageCode, k -> {
                    VarietyStageAccumulator vs = new VarietyStageAccumulator();
                    vs.stageCode = stageCode;
                    GrowthStage stage = stageMap.get(stageCode);
                    vs.stageName = stage != null ? stage.getStageName() : stageCode;
                    vs.stageOrder = stage != null ? stage.getStageOrder() : 999;
                    return vs;
                });
                vsa.trackingCount++;
                if (isAbnormal) vsa.abnormalCount++;
                if (isNormal) vsa.normalCount++;
            }

            if (sowing != null) {
                BatchAccumulator ba = batchMap.computeIfAbsent(sowing.getId(), k -> {
                    BatchAccumulator b = new BatchAccumulator();
                    b.sowingId = sowing.getId();
                    b.varietyId = sowing.getVarietyId();
                    b.varietyName = sowing.getVarietyName();
                    b.sowingQuantity = sowing.getSowingQuantity();
                    b.sowingTime = sowing.getSowingTime();
                    return b;
                });
                ba.trackingCount++;
                if (isAbnormal) {
                    ba.abnormalCount++;
                    ba.hasAbnormal = true;
                }
                if (healthStatus != null && !healthStatus.trim().isEmpty()) {
                    ba.healthStatuses.add(healthStatus);
                }
                if (ba.latestRecordTime == null ||
                        (tracking.getRecordTime() != null && tracking.getRecordTime().isAfter(ba.latestRecordTime))) {
                    ba.latestRecordTime = tracking.getRecordTime();
                    ba.latestHealthStatus = healthStatus;
                    ba.currentStageCode = tracking.getStageCode();
                    ba.currentStageName = tracking.getStageName();
                }
            }

            StageAccumulator sa = stageMapAcc.get(tracking.getStageCode());
            if (sa != null) {
                sa.trackingCount++;
                if (isAbnormal) {
                    sa.abnormalCount++;
                    sa.abnormalTypeCounts.merge(abnormalType, 1, Integer::sum);
                }
                if (isNormal) sa.normalCount++;
            }
        }

        result.setTotalTrackingCount(totalCount);
        result.setAbnormalCount(abnormalCount);
        result.setNormalCount(normalCount);
        result.setUnknownCount(unknownCount);
        result.setAbnormalRate(totalCount > 0 ? (abnormalCount * 100.0 / totalCount) : 0.0);

        List<VarietyHealthDTO> byVariety = new ArrayList<>();
        for (VarietyAccumulator va : varietyMap.values()) {
            VarietyHealthDTO dto = new VarietyHealthDTO();
            dto.setVarietyId(va.varietyId);
            dto.setVarietyName(va.varietyName);
            dto.setTrackingCount(va.trackingCount);
            dto.setAbnormalCount(va.abnormalCount);
            dto.setNormalCount(va.normalCount);
            dto.setAbnormalRate(va.trackingCount > 0 ? (va.abnormalCount * 100.0 / va.trackingCount) : 0.0);
            dto.setAbnormalTypes(new ArrayList<>(va.abnormalTypes));

            List<StageHealthDTO> stageBreakdown = new ArrayList<>();
            for (VarietyStageAccumulator vsa : va.stageMap.values()) {
                StageHealthDTO sdto = new StageHealthDTO();
                sdto.setStageCode(vsa.stageCode);
                sdto.setStageName(vsa.stageName);
                sdto.setStageOrder(vsa.stageOrder);
                sdto.setTrackingCount(vsa.trackingCount);
                sdto.setAbnormalCount(vsa.abnormalCount);
                sdto.setNormalCount(vsa.normalCount);
                sdto.setAbnormalRate(vsa.trackingCount > 0 ? (vsa.abnormalCount * 100.0 / vsa.trackingCount) : 0.0);
                stageBreakdown.add(sdto);
            }
            stageBreakdown.sort(Comparator.comparingInt(StageHealthDTO::getStageOrder));
            dto.setStageBreakdown(stageBreakdown);

            byVariety.add(dto);
        }
        byVariety.sort((a, b) -> Integer.compare(b.getAbnormalCount(), a.getAbnormalCount()));
        result.setByVariety(byVariety);

        List<BatchHealthDTO> byBatch = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (BatchAccumulator ba : batchMap.values()) {
            BatchHealthDTO dto = new BatchHealthDTO();
            dto.setSowingId(ba.sowingId);
            dto.setVarietyId(ba.varietyId);
            dto.setVarietyName(ba.varietyName);
            dto.setSowingQuantity(ba.sowingQuantity);
            dto.setSowingTime(ba.sowingTime);
            if (ba.sowingTime != null) {
                dto.setDaysSinceSowing(ChronoUnit.DAYS.between(ba.sowingTime.toLocalDate(), today));
            }
            dto.setCurrentStageCode(ba.currentStageCode);
            dto.setCurrentStageName(ba.currentStageName);
            dto.setLatestHealthStatus(ba.latestHealthStatus);
            dto.setLatestRecordTime(ba.latestRecordTime);
            dto.setTrackingCount(ba.trackingCount);
            dto.setAbnormalCount(ba.abnormalCount);
            dto.setHasAbnormal(ba.hasAbnormal);
            dto.setHealthStatuses(new ArrayList<>(ba.healthStatuses));
            byBatch.add(dto);
        }
        byBatch.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getHasAbnormal()) && !Boolean.TRUE.equals(b.getHasAbnormal())) return -1;
            if (!Boolean.TRUE.equals(a.getHasAbnormal()) && Boolean.TRUE.equals(b.getHasAbnormal())) return 1;
            return Integer.compare(b.getAbnormalCount(), a.getAbnormalCount());
        });
        result.setByBatch(byBatch);

        List<StageHealthDTO> byStage = new ArrayList<>();
        for (StageAccumulator sa : stageMapAcc.values()) {
            StageHealthDTO dto = new StageHealthDTO();
            dto.setStageCode(sa.stageCode);
            dto.setStageName(sa.stageName);
            dto.setStageOrder(sa.stageOrder);
            dto.setTrackingCount(sa.trackingCount);
            dto.setAbnormalCount(sa.abnormalCount);
            dto.setNormalCount(sa.normalCount);
            dto.setAbnormalRate(sa.trackingCount > 0 ? (sa.abnormalCount * 100.0 / sa.trackingCount) : 0.0);

            List<String> commonTypes = sa.abnormalTypeCounts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            dto.setCommonAbnormalTypes(commonTypes);

            byStage.add(dto);
        }
        byStage.sort(Comparator.comparingInt(StageHealthDTO::getStageOrder));
        result.setByStage(byStage);

        abnormalDetails.sort((a, b) -> b.getRecordTime().compareTo(a.getRecordTime()));
        result.setAbnormalDetails(abnormalDetails);

        return result;
    }

    private int determineSeverity(String healthStatus, String abnormalType) {
        if (healthStatus == null) return 1;
        String upper = healthStatus.toUpperCase();
        String lower = healthStatus.toLowerCase();

        Set<String> severeKeywords = new HashSet<>(Arrays.asList(
                "严重", "重度", "非常", "特别", "大量", "全部", "濒死",
                "SEVERE", "SERIOUS", "CRITICAL", "VERY", "BADLY", "MASSIVE", "DYING"
        ));

        for (String kw : severeKeywords) {
            if (healthStatus.contains(kw) || upper.contains(kw) || lower.contains(kw.toLowerCase())) {
                return 3;
            }
        }

        Set<String> moderateKeywords = new HashSet<>(Arrays.asList(
                "中度", "较多", "明显", "比较", "有些",
                "MODERATE", "OBVIOUS", "RELATIVELY", "QUITE", "SOME"
        ));

        for (String kw : moderateKeywords) {
            if (healthStatus.contains(kw) || upper.contains(kw) || lower.contains(kw.toLowerCase())) {
                return 2;
            }
        }

        if ("徒长".equals(abnormalType) || "发黄".equals(abnormalType) || "缺素".equals(abnormalType)) {
            return 1;
        }

        if ("病弱".equals(abnormalType) || "萎蔫".equals(abnormalType) || "烂根".equals(abnormalType)) {
            return 2;
        }

        return 1;
    }

    public List<BatchHealthDTO> getBatchHealthByVariety(Long varietyId) {
        HealthStatusAggregationDTO aggregation = getAggregation();
        return aggregation.getByBatch().stream()
                .filter(b -> b.getVarietyId() != null && b.getVarietyId().equals(varietyId))
                .collect(Collectors.toList());
    }

    public BatchHealthDTO getBatchHealth(Long sowingId) {
        HealthStatusAggregationDTO aggregation = getAggregation();
        return aggregation.getByBatch().stream()
                .filter(b -> b.getSowingId().equals(sowingId))
                .findFirst()
                .orElse(null);
    }

    public VarietyHealthDTO getVarietyHealth(Long varietyId) {
        HealthStatusAggregationDTO aggregation = getAggregation();
        return aggregation.getByVariety().stream()
                .filter(v -> v.getVarietyId() != null && v.getVarietyId().equals(varietyId))
                .findFirst()
                .orElse(null);
    }

    private static class VarietyAccumulator {
        Long varietyId;
        String varietyName;
        int trackingCount;
        int abnormalCount;
        int normalCount;
        Set<String> abnormalTypes = new LinkedHashSet<>();
        Map<String, VarietyStageAccumulator> stageMap = new LinkedHashMap<>();
    }

    private static class VarietyStageAccumulator {
        String stageCode;
        String stageName;
        int stageOrder;
        int trackingCount;
        int abnormalCount;
        int normalCount;
    }

    private static class BatchAccumulator {
        Long sowingId;
        Long varietyId;
        String varietyName;
        Integer sowingQuantity;
        java.time.LocalDateTime sowingTime;
        String currentStageCode;
        String currentStageName;
        String latestHealthStatus;
        java.time.LocalDateTime latestRecordTime;
        int trackingCount;
        int abnormalCount;
        boolean hasAbnormal;
        Set<String> healthStatuses = new LinkedHashSet<>();
    }

    private static class StageAccumulator {
        String stageCode;
        String stageName;
        int stageOrder;
        int trackingCount;
        int abnormalCount;
        int normalCount;
        Map<String, Integer> abnormalTypeCounts = new LinkedHashMap<>();
    }
}
