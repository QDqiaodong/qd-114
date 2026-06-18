package com.flower.cultivation.service;

import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.GerminationAnomaly;
import com.flower.cultivation.entity.GerminationObservation;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.GerminationAnomalyRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GerminationAnomalyService {

    private final GerminationAnomalyRepository germinationAnomalyRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;
    private final SowingRecordRepository sowingRecordRepository;

    public List<GerminationAnomaly> findBySowingId(Long sowingId) {
        return germinationAnomalyRepository.findBySowingIdOrderByCreateTimeDesc(sowingId);
    }

    public List<GerminationAnomaly> findByVarietyId(Long varietyId) {
        return germinationAnomalyRepository.findByVarietyIdOrderByCreateTimeDesc(varietyId);
    }

    public List<GerminationAnomaly> findByStatus(String status) {
        return germinationAnomalyRepository.findByStatusOrderByCreateTimeDesc(status);
    }

    public GerminationAnomaly findById(Long id) {
        return germinationAnomalyRepository.findById(id).orElse(null);
    }

    public GerminationAnomaly findByObservationId(Long observationId) {
        return germinationAnomalyRepository.findByObservationId(observationId);
    }

    public long countByStatus(String status) {
        return germinationAnomalyRepository.countByStatus(status);
    }

    @Transactional
    public GerminationAnomaly checkAndCreateAnomaly(GerminationObservation observation) {
        FlowerVariety variety = flowerVarietyRepository.findById(observation.getVarietyId()).orElse(null);
        if (variety == null || variety.getBaselineGerminationRate() == null) {
            return null;
        }

        BigDecimal baselineRate = variety.getBaselineGerminationRate();
        BigDecimal actualRate = observation.getGerminationRate();

        if (actualRate == null || actualRate.compareTo(baselineRate) >= 0) {
            GerminationAnomaly existing = germinationAnomalyRepository.findByObservationId(observation.getId());
            if (existing != null && !"RESOLVED".equals(existing.getStatus()) && !"CLOSED".equals(existing.getStatus())) {
                existing.setStatus("RESOLVED");
                existing.setResultNote("发芽率已恢复正常，达到 " + actualRate + "%");
                existing.setHandleTime(LocalDateTime.now());
                germinationAnomalyRepository.save(existing);
                updateSowingBatchStatus(observation.getSowingId());
            }
            return null;
        }

        GerminationAnomaly existing = germinationAnomalyRepository.findByObservationId(observation.getId());
        if (existing != null) {
            existing.setActualGerminationRate(actualRate);
            existing.setRateDiff(baselineRate.subtract(actualRate));
            existing.setAnomalyLevel(determineAnomalyLevel(baselineRate, actualRate));
            return germinationAnomalyRepository.save(existing);
        }

        GerminationAnomaly anomaly = new GerminationAnomaly();
        anomaly.setSowingId(observation.getSowingId());
        anomaly.setVarietyId(observation.getVarietyId());
        anomaly.setVarietyName(observation.getVarietyName());
        anomaly.setObservationId(observation.getId());
        anomaly.setBaselineGerminationRate(baselineRate);
        anomaly.setActualGerminationRate(actualRate);
        anomaly.setRateDiff(baselineRate.subtract(actualRate));
        anomaly.setAnomalyLevel(determineAnomalyLevel(baselineRate, actualRate));
        anomaly.setStatus("PENDING");

        GerminationAnomaly saved = germinationAnomalyRepository.save(anomaly);

        updateSowingBatchStatus(observation.getSowingId());

        return saved;
    }

    private String determineAnomalyLevel(BigDecimal baseline, BigDecimal actual) {
        BigDecimal diff = baseline.subtract(actual);
        if (diff.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "SEVERE";
        } else if (diff.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "MODERATE";
        } else {
            return "MILD";
        }
    }

    private void updateSowingBatchStatus(Long sowingId) {
        SowingRecord sowing = sowingRecordRepository.findById(sowingId).orElse(null);
        if (sowing == null) {
            return;
        }

        List<GerminationAnomaly> pendingAnomalies = germinationAnomalyRepository.findBySowingIdAndStatus(sowingId, "PENDING");
        List<GerminationAnomaly> processingAnomalies = germinationAnomalyRepository.findBySowingIdAndStatus(sowingId, "PROCESSING");

        if (!pendingAnomalies.isEmpty() || !processingAnomalies.isEmpty()) {
            sowing.setBatchStatus("ABNORMAL");
        } else {
            sowing.setBatchStatus("NORMAL");
        }

        sowingRecordRepository.save(sowing);
    }

    @Transactional
    public GerminationAnomaly processAnomaly(Long id, String actionTaken, String handler) {
        GerminationAnomaly anomaly = germinationAnomalyRepository.findById(id).orElse(null);
        if (anomaly == null) {
            throw new RuntimeException("异常记录不存在");
        }

        anomaly.setActionTaken(actionTaken);
        anomaly.setHandler(handler);
        anomaly.setStatus("PROCESSING");
        anomaly.setHandleTime(LocalDateTime.now());

        GerminationAnomaly saved = germinationAnomalyRepository.save(anomaly);
        updateSowingBatchStatus(anomaly.getSowingId());
        return saved;
    }

    @Transactional
    public GerminationAnomaly resolveAnomaly(Long id, String resultNote, String handler) {
        GerminationAnomaly anomaly = germinationAnomalyRepository.findById(id).orElse(null);
        if (anomaly == null) {
            throw new RuntimeException("异常记录不存在");
        }

        anomaly.setResultNote(resultNote);
        anomaly.setHandler(handler);
        anomaly.setStatus("RESOLVED");
        anomaly.setHandleTime(LocalDateTime.now());

        GerminationAnomaly saved = germinationAnomalyRepository.save(anomaly);
        updateSowingBatchStatus(anomaly.getSowingId());
        return saved;
    }

    @Transactional
    public GerminationAnomaly closeAnomaly(Long id) {
        GerminationAnomaly anomaly = germinationAnomalyRepository.findById(id).orElse(null);
        if (anomaly == null) {
            throw new RuntimeException("异常记录不存在");
        }

        anomaly.setStatus("CLOSED");
        anomaly.setHandleTime(LocalDateTime.now());

        GerminationAnomaly saved = germinationAnomalyRepository.save(anomaly);
        updateSowingBatchStatus(anomaly.getSowingId());
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        GerminationAnomaly anomaly = germinationAnomalyRepository.findById(id).orElse(null);
        if (anomaly == null) {
            return;
        }
        Long sowingId = anomaly.getSowingId();
        germinationAnomalyRepository.deleteById(id);
        updateSowingBatchStatus(sowingId);
    }
}
