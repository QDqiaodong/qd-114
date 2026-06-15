package com.flower.cultivation.service;

import com.flower.cultivation.dto.TransplantDetailDTO;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransplantRecordService {

    private final TransplantRecordRepository transplantRecordRepository;
    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;

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

        return dto;
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
