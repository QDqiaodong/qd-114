package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GrowthTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrowthTrackingRepository extends JpaRepository<GrowthTracking, Long> {
    List<GrowthTracking> findBySowingIdOrderByRecordTimeAsc(Long sowingId);
    List<GrowthTracking> findBySowingIdAndStageCode(Long sowingId, String stageCode);
}
