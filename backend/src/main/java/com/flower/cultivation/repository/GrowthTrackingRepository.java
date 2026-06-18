package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GrowthTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrowthTrackingRepository extends JpaRepository<GrowthTracking, Long> {
    List<GrowthTracking> findBySowingIdOrderByRecordTimeAsc(Long sowingId);
    List<GrowthTracking> findBySowingIdAndStageCode(Long sowingId, String stageCode);
    boolean existsBySowingId(Long sowingId);

    List<GrowthTracking> findByHealthStatusIsNotNull();

    @Query("SELECT DISTINCT t.healthStatus FROM GrowthTracking t WHERE t.healthStatus IS NOT NULL")
    List<String> findDistinctHealthStatuses();

    List<GrowthTracking> findByStageCode(String stageCode);

    @Query("SELECT COUNT(t) FROM GrowthTracking t WHERE t.sowingId IN (SELECT s.id FROM SowingRecord s WHERE s.varietyId = :varietyId)")
    int countByVarietyId(Long varietyId);

    @Query("SELECT t FROM GrowthTracking t WHERE t.healthStatus IS NOT NULL AND t.healthStatus NOT IN ('健康', '正常', '良好', '-', '/')")
    List<GrowthTracking> findPotentiallyAbnormalTrackings();
}
