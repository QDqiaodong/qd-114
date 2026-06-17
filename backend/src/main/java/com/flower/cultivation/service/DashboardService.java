package com.flower.cultivation.service;

import com.flower.cultivation.dto.SeedlingRateStatisticsDTO;
import com.flower.cultivation.entity.*;
import com.flower.cultivation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SeedInfoRepository seedInfoRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;
    private final TransplantRecordRepository transplantRecordRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;
    private final GrowthStageCacheService growthStageCacheService;
    private final HealthStatusAggregationService healthStatusAggregationService;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalSeeds", getTotalSeeds());
        result.put("activeSowings", getActiveSowings());
        result.put("expiringSeeds", getExpiringSeeds());
        result.put("pendingTransplants", getPendingTransplants());
        result.put("abnormalHealth", getAbnormalHealth());

        return result;
    }

    private Map<String, Object> getTotalSeeds() {
        Map<String, Object> data = new LinkedHashMap<>();
        Integer totalCount = seedInfoRepository.sumRemainingQuantity();
        data.put("count", totalCount);

        List<SeedInfo> allSeeds = seedInfoRepository.findAllByOrderByCreateTimeDesc();
        Map<String, Map<String, Object>> varietyMap = new LinkedHashMap<>();

        for (SeedInfo seed : allSeeds) {
            String vName = seed.getVarietyName();
            varietyMap.computeIfAbsent(vName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("varietyName", vName);
                m.put("varietyId", seed.getVarietyId());
                m.put("totalQty", 0);
                m.put("seedCount", 0);
                return m;
            });
            Map<String, Object> vInfo = varietyMap.get(vName);
            vInfo.put("totalQty", (Integer) vInfo.get("totalQty") + seed.getRemainingQuantity());
            vInfo.put("seedCount", (Integer) vInfo.get("seedCount") + 1);
        }

        List<Map<String, Object>> varieties = new ArrayList<>(varietyMap.values());
        varieties.sort((a, b) -> ((Integer) b.get("totalQty")).compareTo((Integer) a.get("totalQty")));
        data.put("varieties", varieties);

        List<Map<String, Object>> details = allSeeds.stream().map(seed -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", seed.getId());
            m.put("varietyName", seed.getVarietyName());
            m.put("remainingQuantity", seed.getRemainingQuantity());
            m.put("sourceType", seed.getSourceType());
            m.put("acquireTime", seed.getAcquireTime().toString());
            return m;
        }).collect(Collectors.toList());
        data.put("details", details);

        return data;
    }

    private Map<String, Object> getActiveSowings() {
        Map<String, Object> data = new LinkedHashMap<>();

        List<TransplantRecord> transplants = transplantRecordRepository.findAll();
        Map<Long, Integer> transplantedQtyBySowingId = new HashMap<>();
        for (TransplantRecord t : transplants) {
            transplantedQtyBySowingId.merge(t.getSowingId(),
                    t.getTransplantQuantity() != null ? t.getTransplantQuantity() : 0, Integer::sum);
        }

        List<SowingRecord> allSowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();
        List<SowingRecord> activeList = new ArrayList<>();
        Map<Long, Integer> remainingQtyMap = new HashMap<>();

        for (SowingRecord sowing : allSowings) {
            int sowingQty = sowing.getSowingQuantity() != null ? sowing.getSowingQuantity() : 0;
            int transplantedQty = transplantedQtyBySowingId.getOrDefault(sowing.getId(), 0);
            int remainingQty = Math.max(0, sowingQty - transplantedQty);
            if (remainingQty > 0) {
                activeList.add(sowing);
                remainingQtyMap.put(sowing.getId(), remainingQty);
            }
        }

        data.put("count", activeList.size());

        Map<String, Map<String, Object>> varietyMap = new LinkedHashMap<>();
        for (SowingRecord sowing : activeList) {
            String vName = sowing.getVarietyName();
            int remainingQty = remainingQtyMap.getOrDefault(sowing.getId(), 0);
            varietyMap.computeIfAbsent(vName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("varietyName", vName);
                m.put("varietyId", sowing.getVarietyId());
                m.put("batchCount", 0);
                m.put("totalQty", 0);
                return m;
            });
            Map<String, Object> vInfo = varietyMap.get(vName);
            vInfo.put("batchCount", (Integer) vInfo.get("batchCount") + 1);
            vInfo.put("totalQty", (Integer) vInfo.get("totalQty") + remainingQty);
        }

        List<Map<String, Object>> varieties = new ArrayList<>(varietyMap.values());
        varieties.sort((a, b) -> ((Integer) b.get("batchCount")).compareTo((Integer) a.get("batchCount")));
        data.put("varieties", varieties);

        List<Map<String, Object>> details = activeList.stream().map(sowing -> {
            Map<String, Object> m = new LinkedHashMap<>();
            int remainingQty = remainingQtyMap.getOrDefault(sowing.getId(), 0);
            int transplantedQty = transplantedQtyBySowingId.getOrDefault(sowing.getId(), 0);
            m.put("id", sowing.getId());
            m.put("varietyName", sowing.getVarietyName());
            m.put("sowingQuantity", sowing.getSowingQuantity());
            m.put("remainingQuantity", remainingQty);
            m.put("transplantedQuantity", transplantedQty);
            m.put("sowingTime", sowing.getSowingTime().toString());
            m.put("daysSince", ChronoUnit.DAYS.between(sowing.getSowingTime().toLocalDate(), LocalDate.now()));
            return m;
        }).collect(Collectors.toList());
        data.put("details", details);

        return data;
    }

    private Map<String, Object> getExpiringSeeds() {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);

        List<SeedInfo> candidates = seedInfoRepository
                .findByShelfLifeIsNotNullAndRemainingQuantityGreaterThanOrderByAcquireTimeAsc(0);

        List<SeedInfo> expiringList = new ArrayList<>();
        for (SeedInfo seed : candidates) {
            if (seed.getAcquireTime() != null && seed.getShelfLife() != null) {
                LocalDate expireDate = seed.getAcquireTime().plusMonths(seed.getShelfLife());
                if (!expireDate.isAfter(threshold)) {
                    expiringList.add(seed);
                }
            }
        }

        expiringList.sort((a, b) -> {
            LocalDate d1 = a.getAcquireTime().plusMonths(a.getShelfLife());
            LocalDate d2 = b.getAcquireTime().plusMonths(b.getShelfLife());
            return d1.compareTo(d2);
        });

        data.put("count", expiringList.size());

        Map<String, Map<String, Object>> varietyMap = new LinkedHashMap<>();
        for (SeedInfo seed : expiringList) {
            String vName = seed.getVarietyName();
            LocalDate expireDate = seed.getAcquireTime().plusMonths(seed.getShelfLife());
            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);
            varietyMap.computeIfAbsent(vName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("varietyName", vName);
                m.put("varietyId", seed.getVarietyId());
                m.put("seedCount", 0);
                m.put("totalQty", 0);
                m.put("minDaysLeft", Long.MAX_VALUE);
                return m;
            });
            Map<String, Object> vInfo = varietyMap.get(vName);
            vInfo.put("seedCount", (Integer) vInfo.get("seedCount") + 1);
            vInfo.put("totalQty", (Integer) vInfo.get("totalQty") + seed.getRemainingQuantity());
            if (daysLeft < (Long) vInfo.get("minDaysLeft")) {
                vInfo.put("minDaysLeft", daysLeft);
            }
        }

        List<Map<String, Object>> varieties = new ArrayList<>(varietyMap.values());
        varieties.sort((a, b) -> ((Long) a.get("minDaysLeft")).compareTo((Long) b.get("minDaysLeft")));
        data.put("varieties", varieties);

        List<Map<String, Object>> details = expiringList.stream().map(seed -> {
            Map<String, Object> m = new LinkedHashMap<>();
            LocalDate expireDate = seed.getAcquireTime().plusMonths(seed.getShelfLife());
            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);
            m.put("id", seed.getId());
            m.put("varietyName", seed.getVarietyName());
            m.put("remainingQuantity", seed.getRemainingQuantity());
            m.put("expireDate", expireDate.toString());
            m.put("daysLeft", daysLeft);
            return m;
        }).collect(Collectors.toList());
        data.put("details", details);

        return data;
    }

    private Map<String, Object> getPendingTransplants() {
        Map<String, Object> data = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        List<TransplantRecord> transplants = transplantRecordRepository.findAll();
        Map<Long, Integer> transplantedQtyBySowingId = new HashMap<>();
        for (TransplantRecord t : transplants) {
            transplantedQtyBySowingId.merge(t.getSowingId(),
                    t.getTransplantQuantity() != null ? t.getTransplantQuantity() : 0, Integer::sum);
        }

        List<FlowerVariety> allVarieties = flowerVarietyRepository.findAll();
        Map<Long, Integer> varietySeedlingDays = new HashMap<>();
        for (FlowerVariety v : allVarieties) {
            if (v.getSeedlingDays() != null) {
                varietySeedlingDays.put(v.getId(), v.getSeedlingDays());
            }
        }

        List<SowingRecord> allSowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();
        List<SowingRecord> pendingList = new ArrayList<>();
        Map<Long, Integer> remainingQtyMap = new HashMap<>();

        Set<String> transplantReadyStages = new HashSet<>(Arrays.asList(
                "ROOT_DEVELOPED", "SEEDLING", "TRANSPLANTED", "GROWING", "FLOWERING"
        ));

        for (SowingRecord sowing : allSowings) {
            int sowingQty = sowing.getSowingQuantity() != null ? sowing.getSowingQuantity() : 0;
            int transplantedQty = transplantedQtyBySowingId.getOrDefault(sowing.getId(), 0);
            int remainingQty = Math.max(0, sowingQty - transplantedQty);
            if (remainingQty <= 0) {
                continue;
            }

            boolean ready = false;

            List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowing.getId());
            for (GrowthTracking t : trackings) {
                if (transplantReadyStages.contains(t.getStageCode())) {
                    ready = true;
                    break;
                }
            }

            if (!ready) {
                Integer seedlingDays = varietySeedlingDays.get(sowing.getVarietyId());
                if (seedlingDays != null && seedlingDays > 0) {
                    long daysSince = ChronoUnit.DAYS.between(sowing.getSowingTime().toLocalDate(), today);
                    if (daysSince >= seedlingDays) {
                        ready = true;
                    }
                }
            }

            if (ready) {
                pendingList.add(sowing);
                remainingQtyMap.put(sowing.getId(), remainingQty);
            }
        }

        pendingList.sort((a, b) -> a.getSowingTime().compareTo(b.getSowingTime()));
        data.put("count", pendingList.size());

        Map<String, Map<String, Object>> varietyMap = new LinkedHashMap<>();
        for (SowingRecord sowing : pendingList) {
            String vName = sowing.getVarietyName();
            long daysSince = ChronoUnit.DAYS.between(sowing.getSowingTime().toLocalDate(), today);
            int remainingQty = remainingQtyMap.getOrDefault(sowing.getId(), 0);
            varietyMap.computeIfAbsent(vName, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("varietyName", vName);
                m.put("varietyId", sowing.getVarietyId());
                m.put("batchCount", 0);
                m.put("totalQty", 0);
                m.put("maxDaysSince", 0L);
                return m;
            });
            Map<String, Object> vInfo = varietyMap.get(vName);
            vInfo.put("batchCount", (Integer) vInfo.get("batchCount") + 1);
            vInfo.put("totalQty", (Integer) vInfo.get("totalQty") + remainingQty);
            if (daysSince > (Long) vInfo.get("maxDaysSince")) {
                vInfo.put("maxDaysSince", daysSince);
            }
        }

        List<Map<String, Object>> varieties = new ArrayList<>(varietyMap.values());
        varieties.sort((a, b) -> ((Long) b.get("maxDaysSince")).compareTo((Long) a.get("maxDaysSince")));
        data.put("varieties", varieties);

        List<Map<String, Object>> details = pendingList.stream().map(sowing -> {
            Map<String, Object> m = new LinkedHashMap<>();
            long daysSince = ChronoUnit.DAYS.between(sowing.getSowingTime().toLocalDate(), today);
            int remainingQty = remainingQtyMap.getOrDefault(sowing.getId(), 0);
            int transplantedQty = transplantedQtyBySowingId.getOrDefault(sowing.getId(), 0);
            m.put("id", sowing.getId());
            m.put("varietyName", sowing.getVarietyName());
            m.put("sowingQuantity", sowing.getSowingQuantity());
            m.put("remainingQuantity", remainingQty);
            m.put("transplantedQuantity", transplantedQty);
            m.put("sowingTime", sowing.getSowingTime().toString());
            m.put("daysSince", daysSince);
            return m;
        }).collect(Collectors.toList());
        data.put("details", details);

        return data;
    }

    private Map<String, Object> getAbnormalHealth() {
        Map<String, Object> data = new LinkedHashMap<>();

        com.flower.cultivation.dto.HealthStatusAggregationDTO aggregation =
                healthStatusAggregationService.getAggregation();

        data.put("count", aggregation.getAbnormalCount());
        data.put("totalTrackingCount", aggregation.getTotalTrackingCount());
        data.put("normalCount", aggregation.getNormalCount());
        data.put("unknownCount", aggregation.getUnknownCount());
        data.put("abnormalRate", aggregation.getAbnormalRate());

        List<Map<String, Object>> varieties = aggregation.getByVariety().stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("varietyName", v.getVarietyName());
                    m.put("varietyId", v.getVarietyId());
                    m.put("issueCount", v.getAbnormalCount());
                    m.put("trackingCount", v.getTrackingCount());
                    m.put("abnormalRate", v.getAbnormalRate());
                    m.put("healthStatuses", v.getAbnormalTypes());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("varieties", varieties);

        List<Map<String, Object>> details = aggregation.getAbnormalDetails().stream()
                .map(item -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", item.getTrackingId());
                    m.put("sowingId", item.getSowingId());
                    m.put("varietyName", item.getVarietyName());
                    m.put("stageName", item.getStageName());
                    m.put("healthStatus", item.getHealthStatus());
                    m.put("abnormalType", item.getAbnormalType());
                    m.put("severityLevel", item.getSeverityLevel());
                    m.put("recordTime", item.getRecordTime().toString());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("details", details);

        List<Map<String, Object>> byStage = aggregation.getByStage().stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("stageCode", s.getStageCode());
                    m.put("stageName", s.getStageName());
                    m.put("stageOrder", s.getStageOrder());
                    m.put("trackingCount", s.getTrackingCount());
                    m.put("abnormalCount", s.getAbnormalCount());
                    m.put("abnormalRate", s.getAbnormalRate());
                    m.put("commonAbnormalTypes", s.getCommonAbnormalTypes());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("byStage", byStage);

        List<Map<String, Object>> byBatch = aggregation.getByBatch().stream()
                .filter(b -> Boolean.TRUE.equals(b.getHasAbnormal()))
                .limit(20)
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sowingId", b.getSowingId());
                    m.put("varietyName", b.getVarietyName());
                    m.put("varietyId", b.getVarietyId());
                    m.put("currentStageName", b.getCurrentStageName());
                    m.put("latestHealthStatus", b.getLatestHealthStatus());
                    m.put("abnormalCount", b.getAbnormalCount());
                    m.put("trackingCount", b.getTrackingCount());
                    m.put("daysSinceSowing", b.getDaysSinceSowing());
                    return m;
                })
                .collect(Collectors.toList());
        data.put("byBatch", byBatch);

        return data;
    }

    public List<Map<String, Object>> getGerminationProgress() {
        List<GrowthStage> allStages = growthStageCacheService.getAllStages();
        Map<String, Integer> stageOrderMap = new HashMap<>();
        for (GrowthStage s : allStages) {
            stageOrderMap.put(s.getStageCode(), s.getStageOrder());
        }

        Map<String, String> swimlaneMapping = new LinkedHashMap<>();
        swimlaneMapping.put("PENDING_GERMINATION", "待发芽");
        swimlaneMapping.put("SPROUTING", "出苗");
        swimlaneMapping.put("LEAFING", "长叶");
        swimlaneMapping.put("ACCLIMATING", "缓苗");

        List<SowingRecord> allSowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();

        Map<Long, GrowthTracking> latestTrackingBySowing = new HashMap<>();
        List<GrowthTracking> allTrackings = growthTrackingRepository.findAll();
        for (GrowthTracking t : allTrackings) {
            GrowthTracking existing = latestTrackingBySowing.get(t.getSowingId());
            if (existing == null || t.getRecordTime().isAfter(existing.getRecordTime())) {
                latestTrackingBySowing.put(t.getSowingId(), t);
            }
        }

        Set<String> beyondGerminationStages = new HashSet<>(Arrays.asList(
                "GROWING", "FLOWERING"
        ));

        List<Map<String, Object>> result = new ArrayList<>();
        for (String laneKey : swimlaneMapping.keySet()) {
            Map<String, Object> lane = new LinkedHashMap<>();
            lane.put("laneKey", laneKey);
            lane.put("laneName", swimlaneMapping.get(laneKey));
            lane.put("cards", new ArrayList<Map<String, Object>>());
            result.add(lane);
        }

        Map<String, Map<String, Object>> laneMap = new LinkedHashMap<>();
        for (Map<String, Object> lane : result) {
            laneMap.put((String) lane.get("laneKey"), lane);
        }

        for (SowingRecord sowing : allSowings) {
            GrowthTracking latestTracking = latestTrackingBySowing.get(sowing.getId());

            String currentStageCode;
            if (latestTracking != null) {
                currentStageCode = latestTracking.getStageCode();
            } else {
                currentStageCode = "SOWN";
            }

            if (beyondGerminationStages.contains(currentStageCode)) {
                continue;
            }

            String laneKey = mapStageToSwimlane(currentStageCode);

            Map<String, Object> card = new LinkedHashMap<>();
            card.put("sowingId", sowing.getId());
            card.put("varietyName", sowing.getVarietyName());
            card.put("sowingQuantity", sowing.getSowingQuantity());
            card.put("sowingTime", sowing.getSowingTime().toString());

            BigDecimal envTemp = sowing.getEnvironmentTemp();
            BigDecimal envHumidity = sowing.getEnvironmentHumidity();
            if (latestTracking != null) {
                if (latestTracking.getTemperature() != null) {
                    envTemp = latestTracking.getTemperature();
                }
                if (latestTracking.getHumidity() != null) {
                    envHumidity = latestTracking.getHumidity();
                }
            }
            card.put("temperature", envTemp);
            card.put("humidity", envHumidity);

            if (latestTracking != null) {
                card.put("latestObservationTime", latestTracking.getRecordTime().toString());
                card.put("currentStageCode", latestTracking.getStageCode());
                card.put("currentStageName", latestTracking.getStageName());
            } else {
                card.put("latestObservationTime", null);
                card.put("currentStageCode", "SOWN");
                card.put("currentStageName", "已播种");
            }

            Integer stageOrder = stageOrderMap.getOrDefault(currentStageCode, 0);
            card.put("stageOrder", stageOrder);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) laneMap.get(laneKey).get("cards");
            cards.add(card);
        }

        return result;
    }

    private String mapStageToSwimlane(String stageCode) {
        if (stageCode == null) {
            return "PENDING_GERMINATION";
        }
        switch (stageCode) {
            case "SOWN":
            case "SEED_STORED":
                return "PENDING_GERMINATION";
            case "SPROUTING":
            case "COTYLEDON":
                return "SPROUTING";
            case "TRUE_LEAF":
            case "SEEDLING":
            case "ROOT_DEVELOPED":
                return "LEAFING";
            case "TRANSPLANTED":
                return "ACCLIMATING";
            default:
                return "PENDING_GERMINATION";
        }
    }

    public List<Map<String, Object>> getGrowthTimeline() {
        List<SowingRecord> allSowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();
        List<Map<String, Object>> result = new ArrayList<>();

        for (SowingRecord sowing : allSowings) {
            Map<String, Object> sowingTimeline = new LinkedHashMap<>();
            sowingTimeline.put("sowingId", sowing.getId());
            sowingTimeline.put("varietyName", sowing.getVarietyName());
            sowingTimeline.put("varietyId", sowing.getVarietyId());
            sowingTimeline.put("sowingTime", sowing.getSowingTime().toString());
            sowingTimeline.put("sowingQuantity", sowing.getSowingQuantity());

            List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowing.getId());
            List<Map<String, Object>> timelineEvents = new ArrayList<>();

            Map<String, Object> sowEvent = new LinkedHashMap<>();
            sowEvent.put("eventType", "SOWN");
            sowEvent.put("eventName", "播种");
            sowEvent.put("recordTime", sowing.getSowingTime().toString());
            sowEvent.put("plantHeight", null);
            sowEvent.put("leafCount", null);
            sowEvent.put("rootDevelopment", null);
            sowEvent.put("healthStatus", null);
            sowEvent.put("stageCode", "SOWN");
            sowEvent.put("stageName", "已播种");
            timelineEvents.add(sowEvent);

            for (GrowthTracking tracking : trackings) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("eventType", "TRACKING");
                event.put("eventName", tracking.getStageName());
                event.put("recordTime", tracking.getRecordTime().toString());
                event.put("plantHeight", tracking.getPlantHeight());
                event.put("leafCount", tracking.getLeafCount());
                event.put("rootDevelopment", tracking.getRootDevelopment());
                event.put("healthStatus", tracking.getHealthStatus());
                event.put("stageCode", tracking.getStageCode());
                event.put("stageName", tracking.getStageName());
                event.put("temperature", tracking.getTemperature());
                event.put("humidity", tracking.getHumidity());
                event.put("notes", tracking.getNotes());
                timelineEvents.add(event);
            }

            sowingTimeline.put("events", timelineEvents);
            sowingTimeline.put("eventCount", timelineEvents.size());

            if (!trackings.isEmpty()) {
                GrowthTracking latest = trackings.get(trackings.size() - 1);
                sowingTimeline.put("currentStageCode", latest.getStageCode());
                sowingTimeline.put("currentStageName", latest.getStageName());
                sowingTimeline.put("latestPlantHeight", latest.getPlantHeight());
                sowingTimeline.put("latestLeafCount", latest.getLeafCount());
                sowingTimeline.put("latestHealthStatus", latest.getHealthStatus());
            } else {
                sowingTimeline.put("currentStageCode", "SOWN");
                sowingTimeline.put("currentStageName", "已播种");
                sowingTimeline.put("latestPlantHeight", null);
                sowingTimeline.put("latestLeafCount", null);
                sowingTimeline.put("latestHealthStatus", null);
            }

            long daysSince = ChronoUnit.DAYS.between(sowing.getSowingTime().toLocalDate(), LocalDate.now());
            sowingTimeline.put("daysSinceSowing", daysSince);

            result.add(sowingTimeline);
        }

        return result;
    }

    public Map<String, Object> getSeedVitalityCalendar() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        List<SeedInfo> allSeeds = seedInfoRepository.findByShelfLifeIsNotNullAndRemainingQuantityGreaterThanOrderByAcquireTimeAsc(0);

        List<Map<String, Object>> seedEvents = new ArrayList<>();
        Map<String, List<Map<String, Object>>> dateEventsMap = new LinkedHashMap<>();

        for (SeedInfo seed : allSeeds) {
            if (seed.getAcquireTime() == null || seed.getShelfLife() == null) {
                continue;
            }

            LocalDate acquireDate = seed.getAcquireTime();
            LocalDate expireDate = acquireDate.plusMonths(seed.getShelfLife());
            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);

            String vitalityStatus;
            if (daysLeft < 0) {
                vitalityStatus = "EXPIRED";
            } else if (daysLeft <= 30) {
                vitalityStatus = "EXPIRING_SOON";
            } else {
                vitalityStatus = "VITAL";
            }

            LocalDate optimalStart = acquireDate;
            LocalDate optimalEnd = acquireDate.plusMonths(seed.getShelfLife() * 2 / 3);
            if (optimalEnd.isAfter(expireDate)) {
                optimalEnd = expireDate;
            }

            Map<String, Object> seedInfo = new LinkedHashMap<>();
            seedInfo.put("seedId", seed.getId());
            seedInfo.put("varietyName", seed.getVarietyName());
            seedInfo.put("varietyId", seed.getVarietyId());
            seedInfo.put("storageLocation", seed.getStorageLocation());
            seedInfo.put("remainingQuantity", seed.getRemainingQuantity());
            seedInfo.put("initialQuantity", seed.getInitialQuantity());
            seedInfo.put("acquireTime", acquireDate.toString());
            seedInfo.put("expireDate", expireDate.toString());
            seedInfo.put("shelfLifeMonths", seed.getShelfLife());
            seedInfo.put("daysLeft", daysLeft);
            seedInfo.put("vitalityStatus", vitalityStatus);
            seedInfo.put("optimalStart", optimalStart.toString());
            seedInfo.put("optimalEnd", optimalEnd.toString());
            seedInfo.put("sourceType", seed.getSourceType());
            seedEvents.add(seedInfo);

            String expireKey = expireDate.toString();
            dateEventsMap.computeIfAbsent(expireKey, k -> new ArrayList<>()).add(seedInfo);

            String acquireKey = acquireDate.toString();
            if (!acquireKey.equals(expireKey)) {
                Map<String, Object> acquireEvent = new LinkedHashMap<>(seedInfo);
                acquireEvent.put("eventType", "ACQUIRE");
                dateEventsMap.computeIfAbsent(acquireKey, k -> new ArrayList<>()).add(acquireEvent);
            }
        }

        List<Map<String, Object>> sortedSeeds = new ArrayList<>(seedEvents);
        sortedSeeds.sort((a, b) -> {
            Long d1 = (Long) a.get("daysLeft");
            Long d2 = (Long) b.get("daysLeft");
            return d1.compareTo(d2);
        });

        result.put("today", today.toString());
        result.put("seeds", sortedSeeds);
        result.put("dateEvents", dateEventsMap);

        long vitalCount = sortedSeeds.stream().filter(s -> "VITAL".equals(s.get("vitalityStatus"))).count();
        long expiringSoonCount = sortedSeeds.stream().filter(s -> "EXPIRING_SOON".equals(s.get("vitalityStatus"))).count();
        long expiredCount = sortedSeeds.stream().filter(s -> "EXPIRED".equals(s.get("vitalityStatus"))).count();

        result.put("vitalCount", vitalCount);
        result.put("expiringSoonCount", expiringSoonCount);
        result.put("expiredCount", expiredCount);

        return result;
    }

    public List<SeedlingRateStatisticsDTO> getSeedlingRateStatistics() {
        List<SowingRecord> allSowings = sowingRecordRepository.findAll();
        List<TransplantRecord> allTransplants = transplantRecordRepository.findAll();
        List<GrowthTracking> allTrackings = growthTrackingRepository.findAll();
        List<SeedInfo> allSeeds = seedInfoRepository.findAll();

        Map<Long, SeedInfo> seedInfoMap = new HashMap<>();
        for (SeedInfo seed : allSeeds) {
            seedInfoMap.put(seed.getId(), seed);
        }

        Map<Long, List<TransplantRecord>> transplantsBySowingId = new HashMap<>();
        for (TransplantRecord t : allTransplants) {
            transplantsBySowingId.computeIfAbsent(t.getSowingId(), k -> new ArrayList<>()).add(t);
        }

        Map<Long, List<GrowthTracking>> trackingsBySowingId = new HashMap<>();
        for (GrowthTracking t : allTrackings) {
            trackingsBySowingId.computeIfAbsent(t.getSowingId(), k -> new ArrayList<>()).add(t);
        }

        Map<String, SeedlingRateStatisticsDTO> groupedMap = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        for (SowingRecord sowing : allSowings) {
            SeedInfo seedInfo = seedInfoMap.get(sowing.getSeedId());
            String sourceType = seedInfo != null ? seedInfo.getSourceType() : "未知";
            String sowingMonth = sowing.getSowingTime() != null
                    ? YearMonth.from(sowing.getSowingTime()).format(monthFormatter)
                    : "未知";

            String key = sowing.getVarietyId() + "|" + sourceType + "|" + sowingMonth;

            SeedlingRateStatisticsDTO stat = groupedMap.computeIfAbsent(key, k -> {
                SeedlingRateStatisticsDTO s = new SeedlingRateStatisticsDTO();
                s.setVarietyId(sowing.getVarietyId());
                s.setVarietyName(sowing.getVarietyName());
                s.setSourceType(sourceType);
                s.setSowingMonth(sowingMonth);
                s.setTotalSowingQuantity(0);
                s.setTotalTransplantQuantity(0);
                s.setBatchCount(0);
                return s;
            });

            stat.setTotalSowingQuantity(stat.getTotalSowingQuantity() +
                    (sowing.getSowingQuantity() != null ? sowing.getSowingQuantity() : 0));
            stat.setBatchCount(stat.getBatchCount() + 1);

            List<TransplantRecord> sowingTransplants = transplantsBySowingId.getOrDefault(sowing.getId(), Collections.emptyList());
            int transplantSum = sowingTransplants.stream()
                    .mapToInt(t -> t.getTransplantQuantity() != null ? t.getTransplantQuantity() : 0)
                    .sum();
            stat.setTotalTransplantQuantity(stat.getTotalTransplantQuantity() + transplantSum);
        }

        List<Integer> globalGerminationDays = new ArrayList<>();
        Map<String, List<Integer>> germinationDaysByGroup = new HashMap<>();
        for (SowingRecord sowing : allSowings) {
            SeedInfo seedInfo = seedInfoMap.get(sowing.getSeedId());
            String sourceType = seedInfo != null ? seedInfo.getSourceType() : "未知";
            String sowingMonth = sowing.getSowingTime() != null
                    ? YearMonth.from(sowing.getSowingTime()).format(monthFormatter)
                    : "未知";
            String key = sowing.getVarietyId() + "|" + sourceType + "|" + sowingMonth;

            List<GrowthTracking> trackings = trackingsBySowingId.getOrDefault(sowing.getId(), Collections.emptyList());
            Optional<GrowthTracking> sproutingOpt = trackings.stream()
                    .filter(t -> "SPROUTING".equals(t.getStageCode()))
                    .min(Comparator.comparing(GrowthTracking::getRecordTime));

            if (sproutingOpt.isPresent() && sowing.getSowingTime() != null) {
                long days = Duration.between(sowing.getSowingTime(), sproutingOpt.get().getRecordTime()).toDays();
                int daysInt = (int) Math.max(days, 0);
                globalGerminationDays.add(daysInt);
                germinationDaysByGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(daysInt);
            }
        }

        for (Map.Entry<String, SeedlingRateStatisticsDTO> entry : groupedMap.entrySet()) {
            SeedlingRateStatisticsDTO stat = entry.getValue();
            String key = entry.getKey();

            if (stat.getTotalSowingQuantity() != null && stat.getTotalSowingQuantity() > 0) {
                BigDecimal rate = BigDecimal.valueOf(stat.getTotalTransplantQuantity())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(stat.getTotalSowingQuantity()), 1, RoundingMode.HALF_UP);
                stat.setSeedlingRate(rate);
            } else {
                stat.setSeedlingRate(BigDecimal.ZERO);
            }

            List<Integer> groupDays = germinationDaysByGroup.getOrDefault(key, Collections.emptyList());
            if (!groupDays.isEmpty()) {
                double avg = groupDays.stream().mapToInt(Integer::intValue).average().orElse(0.0);
                stat.setAverageGerminationDays(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
            } else {
                stat.setAverageGerminationDays(null);
            }
        }

        List<SeedlingRateStatisticsDTO> result = new ArrayList<>(groupedMap.values());
        result.sort((a, b) -> {
            int monthCompare = (b.getSowingMonth() != null ? b.getSowingMonth() : "")
                    .compareTo(a.getSowingMonth() != null ? a.getSowingMonth() : "");
            if (monthCompare != 0) return monthCompare;
            int varietyCompare = (a.getVarietyName() != null ? a.getVarietyName() : "")
                    .compareTo(b.getVarietyName() != null ? b.getVarietyName() : "");
            if (varietyCompare != 0) return varietyCompare;
            return (a.getSourceType() != null ? a.getSourceType() : "")
                    .compareTo(b.getSourceType() != null ? b.getSourceType() : "");
        });

        return result;
    }
}
