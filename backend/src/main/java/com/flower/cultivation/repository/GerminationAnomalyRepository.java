package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GerminationAnomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GerminationAnomalyRepository extends JpaRepository<GerminationAnomaly, Long> {

    List<GerminationAnomaly> findBySowingIdOrderByCreateTimeDesc(Long sowingId);

    List<GerminationAnomaly> findByVarietyIdOrderByCreateTimeDesc(Long varietyId);

    List<GerminationAnomaly> findByStatusOrderByCreateTimeDesc(String status);

    List<GerminationAnomaly> findBySowingIdAndStatus(Long sowingId, String status);

    boolean existsByObservationId(Long observationId);

    GerminationAnomaly findByObservationId(Long observationId);

    long countByStatus(String status);
}
