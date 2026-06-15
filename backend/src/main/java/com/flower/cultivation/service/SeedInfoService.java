package com.flower.cultivation.service;

import com.flower.cultivation.dto.SeedRiskItemDTO;
import com.flower.cultivation.dto.SeedRiskReportDTO;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.repository.SeedInfoRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedInfoService {

    private final SeedInfoRepository seedInfoRepository;
    private final SowingRecordRepository sowingRecordRepository;

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
