package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GerminationObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GerminationObservationRepository extends JpaRepository<GerminationObservation, Long> {
    List<GerminationObservation> findBySowingIdOrderByObservationDateAsc(Long sowingId);
    boolean existsBySowingIdAndObservationDate(Long sowingId, LocalDate observationDate);
    List<GerminationObservation> findByVarietyIdOrderByObservationDateAsc(Long varietyId);

    @Modifying
    @Query("UPDATE GerminationObservation g SET g.varietyName = :varietyName WHERE g.varietyId = :varietyId")
    int updateVarietyNameByVarietyId(Long varietyId, String varietyName);
}
