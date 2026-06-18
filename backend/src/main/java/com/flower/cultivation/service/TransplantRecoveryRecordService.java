package com.flower.cultivation.service;

import com.flower.cultivation.dto.TransplantRecoveryDetailDTO;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.entity.TransplantRecoveryRecord;
import com.flower.cultivation.repository.TransplantRecordRepository;
import com.flower.cultivation.repository.TransplantRecoveryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransplantRecoveryRecordService {

    private final TransplantRecoveryRecordRepository recoveryRecordRepository;
    private final TransplantRecordRepository transplantRecordRepository;

    public List<TransplantRecoveryRecord> findByTransplantId(Long transplantId) {
        return recoveryRecordRepository.findByTransplantIdOrderByRecordDateAsc(transplantId);
    }

    public List<TransplantRecoveryRecord> findByTransplantIdDesc(Long transplantId) {
        return recoveryRecordRepository.findByTransplantIdOrderByRecordDateDesc(transplantId);
    }

    public List<TransplantRecoveryRecord> findBySowingId(Long sowingId) {
        return recoveryRecordRepository.findBySowingIdOrderByRecordDateAsc(sowingId);
    }

    public TransplantRecoveryRecord findById(Long id) {
        return recoveryRecordRepository.findById(id).orElse(null);
    }

    public TransplantRecoveryRecord findLatestByTransplantId(Long transplantId) {
        return recoveryRecordRepository.findFirstByTransplantIdOrderByRecordDateDesc(transplantId);
    }

    @Transactional
    public TransplantRecoveryRecord save(TransplantRecoveryRecord record) {
        TransplantRecord transplant = transplantRecordRepository.findById(record.getTransplantId()).orElse(null);
        if (transplant == null) {
            throw new RuntimeException("移栽记录不存在");
        }

        if (record.getRecordDate() == null) {
            record.setRecordDate(LocalDate.now());
        }

        if (record.getRecordDate().isBefore(transplant.getTransplantTime().toLocalDate())) {
            throw new RuntimeException("记录日期不能早于移栽日期");
        }

        if (record.getRecoveryDays() == null) {
            long days = ChronoUnit.DAYS.between(transplant.getTransplantTime().toLocalDate(), record.getRecordDate());
            record.setRecoveryDays((int) days);
        }

        if (record.getId() == null) {
            if (recoveryRecordRepository.existsByTransplantIdAndRecordDate(
                    record.getTransplantId(), record.getRecordDate())) {
                throw new RuntimeException("该移栽记录在此日期已有恢复记录");
            }
        } else {
            TransplantRecoveryRecord existing = findById(record.getId());
            if (existing != null && !existing.getRecordDate().equals(record.getRecordDate())) {
                if (recoveryRecordRepository.existsByTransplantIdAndRecordDate(
                        record.getTransplantId(), record.getRecordDate())) {
                    throw new RuntimeException("该移栽记录在此日期已有恢复记录");
                }
            }
        }

        record.setSowingId(transplant.getSowingId());
        record.setVarietyId(transplant.getVarietyId());
        record.setVarietyName(transplant.getVarietyName());

        if (record.getWateringDone() == null) {
            record.setWateringDone(false);
        }

        return recoveryRecordRepository.save(record);
    }

    @Transactional
    public void deleteById(Long id) {
        recoveryRecordRepository.deleteById(id);
    }

    public TransplantRecoveryDetailDTO getRecoveryDetail(Long transplantId) {
        TransplantRecord transplant = transplantRecordRepository.findById(transplantId).orElse(null);
        if (transplant == null) {
            return null;
        }

        List<TransplantRecoveryRecord> records = findByTransplantId(transplantId);

        TransplantRecoveryDetailDTO dto = new TransplantRecoveryDetailDTO();
        dto.setTransplantId(transplantId);
        dto.setSowingId(transplant.getSowingId());
        dto.setVarietyId(transplant.getVarietyId());
        dto.setVarietyName(transplant.getVarietyName());
        dto.setTransplantTime(transplant.getTransplantTime());
        dto.setPotSpecification(transplant.getPotSpecification());
        dto.setRecoveryTips(transplant.getRecoveryTips());
        dto.setLightRequirement(transplant.getLightRequirement());
        dto.setWateringFrequency(transplant.getWateringFrequency());
        dto.setTransplantQuantity(transplant.getTransplantQuantity());
        dto.setRecords(records);

        LocalDate today = LocalDate.now();
        long daysSinceTransplant = ChronoUnit.DAYS.between(transplant.getTransplantTime().toLocalDate(), today);
        dto.setDaysSinceTransplant(daysSinceTransplant);

        int wateringCount = 0;
        String latestLeafStatus = null;
        String latestHealthStatus = null;
        boolean hasAbnormal = false;

        for (TransplantRecoveryRecord r : records) {
            if (Boolean.TRUE.equals(r.getWateringDone())) {
                wateringCount++;
            }
            latestLeafStatus = r.getLeafStatus();
            latestHealthStatus = r.getHealthStatus();

            String status = r.getHealthStatus();
            if (status != null && (status.contains("病") || status.contains("弱") || status.contains("萎") ||
                    status.contains("黄") || status.contains("差"))) {
                hasAbnormal = true;
            }
        }

        dto.setWateringCount(wateringCount);
        dto.setLatestLeafStatus(latestLeafStatus);
        dto.setLatestHealthStatus(latestHealthStatus);
        dto.setHasAbnormal(hasAbnormal);

        if (!records.isEmpty()) {
            TransplantRecoveryRecord latest = records.get(records.size() - 1);
            dto.setLatestRecordDate(latest.getRecordDate());
        }

        if (daysSinceTransplant <= 7) {
            dto.setRecoveryStage("ACCLIMATING");
            dto.setRecoveryStageText("缓苗期");
        } else if (daysSinceTransplant <= 14) {
            dto.setRecoveryStage("RECOVERING");
            dto.setRecoveryStageText("恢复中");
        } else {
            dto.setRecoveryStage("RECOVERED");
            dto.setRecoveryStageText("已恢复");
        }

        return dto;
    }
}
