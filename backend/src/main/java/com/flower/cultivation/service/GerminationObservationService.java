package com.flower.cultivation.service;

import com.flower.cultivation.entity.GerminationObservation;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.GerminationObservationRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GerminationObservationService {

    private final GerminationObservationRepository germinationObservationRepository;
    private final SowingRecordRepository sowingRecordRepository;

    public List<GerminationObservation> findBySowingId(Long sowingId) {
        return germinationObservationRepository.findBySowingIdOrderByObservationDateAsc(sowingId);
    }

    public List<GerminationObservation> findByVarietyId(Long varietyId) {
        return germinationObservationRepository.findByVarietyIdOrderByObservationDateAsc(varietyId);
    }

    public GerminationObservation findById(Long id) {
        return germinationObservationRepository.findById(id).orElse(null);
    }

    @Transactional
    public GerminationObservation save(GerminationObservation observation) {
        SowingRecord sowing = sowingRecordRepository.findById(observation.getSowingId()).orElse(null);
        if (sowing == null) {
            throw new RuntimeException("播种记录不存在");
        }

        if (observation.getId() == null) {
            if (germinationObservationRepository.existsBySowingIdAndObservationDate(
                    observation.getSowingId(), observation.getObservationDate())) {
                throw new RuntimeException("该播种记录在此日期已有观察记录");
            }
        } else {
            GerminationObservation existing = findById(observation.getId());
            if (existing != null && !existing.getObservationDate().equals(observation.getObservationDate())) {
                if (germinationObservationRepository.existsBySowingIdAndObservationDate(
                        observation.getSowingId(), observation.getObservationDate())) {
                    throw new RuntimeException("该播种记录在此日期已有观察记录");
                }
            }
        }

        observation.setVarietyId(sowing.getVarietyId());
        observation.setVarietyName(sowing.getVarietyName());

        int total = observation.getGerminatedCount() + observation.getNotGerminatedCount();
        if (total > 0) {
            BigDecimal rate = BigDecimal.valueOf(observation.getGerminatedCount())
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            observation.setGerminationRate(rate);
        } else {
            observation.setGerminationRate(BigDecimal.ZERO);
        }

        if (observation.getGerminatedCount() > sowing.getSowingQuantity()) {
            throw new RuntimeException("发芽数量不能超过播种数量");
        }

        return germinationObservationRepository.save(observation);
    }

    @Transactional
    public void deleteById(Long id) {
        germinationObservationRepository.deleteById(id);
    }
}
