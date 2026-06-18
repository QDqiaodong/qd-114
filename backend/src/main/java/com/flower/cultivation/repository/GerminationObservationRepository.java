package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GerminationObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GerminationObservationRepository extends JpaRepository<GerminationObservation, Long> {
    List<GerminationObservation> findBySowingIdOrderByObservationDateAsc(Long sowingId);
    boolean existsBySowingIdAndObservationDate(Long sowingId, LocalDate observationDate);
    List<GerminationObservation> findByVarietyIdOrderByObservationDateAsc(Long varietyId);
}
