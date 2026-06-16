package com.flower.cultivation.service;

import com.flower.cultivation.dto.SeedDetailDTO;
import com.flower.cultivation.dto.SeedRiskItemDTO;
import com.flower.cultivation.dto.SeedRiskReportDTO;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SeedInfoRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeedInfoService {

    private final SeedInfoRepository seedInfoRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;
    private final GrowthTrackingRepository growthTrackingRepository;

    public List<SeedInfo> findAll() {
        return seedInfoRepository.findAllByOrderByCreateTimeDesc();
    }

    public SeedInfo findById(Long id) {
        return seedInfoRepository.findById(id).orElse(null);
    }

    public SeedDetailDTO getSeedDetail(Long id) {
        SeedInfo seed = findById(id);
        if (seed == null) {
            return null;
        }

        SeedDetailDTO dto = new SeedDetailDTO();
        dto.setId(seed.getId());
        dto.setVarietyId(seed.getVarietyId());
        dto.setVarietyName(seed.getVarietyName());
        dto.setRemainingQuantity(seed.getRemainingQuantity());
        dto.setInitialQuantity(seed.getInitialQuantity());
        dto.setSourceType(seed.getSourceType());
        dto.setSourceTypeName("PURCHASE".equals(seed.getSourceType()) ? "购入" : "采收");
        dto.setAcquireTime(seed.getAcquireTime());
        dto.setShelfLife(seed.getShelfLife());
        dto.setSupplier(seed.getSupplier());
        dto.setStorageLocation(seed.getStorageLocation());

        if (seed.getAcquireTime() != null && seed.getShelfLife() != null) {
            dto.setExpireDate(seed.getAcquireTime().plusMonths(seed.getShelfLife()));
        }

        FlowerVariety variety = flowerVarietyRepository.findById(seed.getVarietyId()).orElse(null);
        if (variety != null) {
            dto.setGerminationDays(variety.getGerminationDays());
        }

        dto.setGerminationHistory(buildGerminationHistory(seed.getVarietyId()));

        return dto;
    }

    private SeedDetailDTO.GerminationHistory buildGerminationHistory(Long varietyId) {
        List<SowingRecord> sowings = sowingRecordRepository.findByVarietyId(varietyId);
        if (sowings.isEmpty()) {
            return null;
        }

        List<Long> sowingIds = sowings.stream().map(SowingRecord::getId).collect(Collectors.toList());
        Map<Long, GrowthTracking> germinationTrackingMap = sowingIds.stream()
                .map(sowingId -> growthTrackingRepository.findBySowingIdAndStageCode(sowingId, "GERMINATION"))
                .filter(list -> !list.isEmpty())
                .collect(Collectors.toMap(
                        list -> list.get(0).getSowingId(),
                        list -> list.get(0),
                        (existing, replacement) -> existing
                ));

        List<SeedDetailDTO.GerminationRecord> records = sowings.stream()
                .filter(sowing -> germinationTrackingMap.containsKey(sowing.getId()))
                .map(sowing -> {
                    GrowthTracking tracking = germinationTrackingMap.get(sowing.getId());
                    SeedDetailDTO.GerminationRecord record = new SeedDetailDTO.GerminationRecord();
                    record.setSowingId(sowing.getId());
                    record.setSowingDate(sowing.getSowingTime().toLocalDate());
                    record.setSowingQuantity(sowing.getSowingQuantity());
                    record.setEstimatedSurvival(tracking.getEstimatedSurvival());

                    if (sowing.getSowingQuantity() != null && sowing.getSowingQuantity() > 0
                            && tracking.getEstimatedSurvival() != null) {
                        BigDecimal rate = BigDecimal.valueOf(tracking.getEstimatedSurvival())
                                .divide(BigDecimal.valueOf(sowing.getSowingQuantity()), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        record.setGerminationRate(rate);
                    }

                    return record;
                })
                .filter(r -> r.getGerminationRate() != null)
                .sorted(Comparator.comparing(SeedDetailDTO.GerminationRecord::getSowingDate).reversed())
                .limit(3)
                .collect(Collectors.toList());

        SeedDetailDTO.GerminationHistory history = new SeedDetailDTO.GerminationHistory();
        history.setTotalSowings(records.size());

        if (!records.isEmpty()) {
            BigDecimal avgRate = records.stream()
                    .map(SeedDetailDTO.GerminationRecord::getGerminationRate)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(records.size()), 2, RoundingMode.HALF_UP);
            history.setAverageGerminationRate(avgRate);
        }

        history.setRecentRecords(records);

        return history;
    }

    public List<SeedInfo> findByVarietyId(Long varietyId) {
        return seedInfoRepository.findByVarietyId(varietyId);
    }

    @Transactional
    public SeedInfo save(SeedInfo seedInfo) {
        if (seedInfo.getId() == null && seedInfo.getRemainingQuantity() == null) {
            seedInfo.setRemainingQuantity(seedInfo.getInitialQuantity());
        }
        if (seedInfo.getId() != null) {
            if (seedInfo.getRemainingQuantity() != null && seedInfo.getInitialQuantity() != null
                    && seedInfo.getRemainingQuantity() > seedInfo.getInitialQuantity()) {
                throw new RuntimeException("剩余数量不能大于初始数量");
            }
            List<SowingRecord> sowingRecords = sowingRecordRepository.findBySeedId(seedInfo.getId());
            int totalSown = sowingRecords.stream().mapToInt(SowingRecord::getSowingQuantity).sum();
            if (seedInfo.getInitialQuantity() != null && seedInfo.getRemainingQuantity() != null
                    && seedInfo.getInitialQuantity() - seedInfo.getRemainingQuantity() < totalSown) {
                throw new RuntimeException(
                        String.format("初始数量修改无效：已播种%d粒，剩余%d粒，初始数量不能少于%d",
                                totalSown, seedInfo.getRemainingQuantity(), totalSown + seedInfo.getRemainingQuantity()));
            }
        }
        return seedInfoRepository.save(seedInfo);
    }

    @Transactional
    public void deleteById(Long id) {
        if (sowingRecordRepository.existsBySeedId(id)) {
            throw new RuntimeException("该种子存在播种记录引用，无法删除");
        }
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

    @Transactional
    public boolean increaseQuantity(Long id, int quantity) {
        SeedInfo seed = findById(id);
        if (seed != null) {
            seed.setRemainingQuantity(seed.getRemainingQuantity() + quantity);
            seedInfoRepository.save(seed);
            return true;
        }
        return false;
    }

    public SeedRiskReportDTO getShelfLifeRisk() {
        LocalDate today = LocalDate.now();
        List<SeedInfo> candidates = seedInfoRepository
                .findByShelfLifeIsNotNullAndRemainingQuantityGreaterThanOrderByAcquireTimeAsc(0);

        List<SeedRiskItemDTO> expiredList = new ArrayList<>();
        List<SeedRiskItemDTO> expiringList = new ArrayList<>();
        List<SeedRiskItemDTO> overstockedList = new ArrayList<>();

        for (SeedInfo seed : candidates) {
            if (seed.getAcquireTime() == null || seed.getShelfLife() == null) {
                continue;
            }

            LocalDate expireDate = seed.getAcquireTime().plusMonths(seed.getShelfLife());
            long daysLeft = ChronoUnit.DAYS.between(today, expireDate);
            long shelfLifeDays = ChronoUnit.DAYS.between(seed.getAcquireTime(), expireDate);
            long elapsedDays = ChronoUnit.DAYS.between(seed.getAcquireTime(), today);
            double elapsedRatio = shelfLifeDays > 0 ? (double) elapsedDays / shelfLifeDays : 1.0;
            double remainingRatio = (double) seed.getRemainingQuantity() / seed.getInitialQuantity();

            if (daysLeft < 0) {
                SeedRiskItemDTO item = buildRiskItem(seed, expireDate, daysLeft,
                        "EXPIRED", "已过期" + Math.abs(daysLeft) + "天");
                expiredList.add(item);
            } else if (daysLeft <= 30) {
                SeedRiskItemDTO item = buildRiskItem(seed, expireDate, daysLeft,
                        "EXPIRING", "距过期仅剩" + daysLeft + "天");
                expiringList.add(item);
            } else if (elapsedRatio > 0.5 && remainingRatio > 0.8 && seed.getRemainingQuantity() > 50) {
                SeedRiskItemDTO item = buildRiskItem(seed, expireDate, daysLeft,
                        "OVERSTOCKED", "已过半保质期但剩余量仍占初始量" + String.format("%.0f%%", remainingRatio * 100));
                overstockedList.add(item);
            }
        }

        expiredList.sort((a, b) -> Long.compare(a.getDaysLeft(), b.getDaysLeft()));
        expiringList.sort((a, b) -> Long.compare(a.getDaysLeft(), b.getDaysLeft()));
        overstockedList.sort((a, b) -> Integer.compare(b.getRemainingQuantity(), a.getRemainingQuantity()));

        return new SeedRiskReportDTO(expiredList, expiringList, overstockedList,
                expiredList.size(), expiringList.size(), overstockedList.size());
    }

    private SeedRiskItemDTO buildRiskItem(SeedInfo seed, LocalDate expireDate, long daysLeft,
                                           String riskLevel, String riskReason) {
        return new SeedRiskItemDTO(
                seed.getId(),
                seed.getVarietyName(),
                seed.getVarietyId(),
                seed.getRemainingQuantity(),
                seed.getInitialQuantity(),
                seed.getAcquireTime(),
                expireDate,
                seed.getShelfLife(),
                daysLeft,
                seed.getStorageLocation(),
                riskLevel,
                riskReason
        );
    }
}
