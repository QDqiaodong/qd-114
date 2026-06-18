package com.flower.cultivation.repository;

import com.flower.cultivation.entity.SeedlingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeedlingPlanRepository extends JpaRepository<SeedlingPlan, Long> {
    List<SeedlingPlan> findAllByOrderByPlannedSowDateAsc();
    List<SeedlingPlan> findBySeedId(Long seedId);
    List<SeedlingPlan> findByVarietyId(Long varietyId);
    List<SeedlingPlan> findByStatus(String status);
    boolean existsBySeedId(Long seedId);

    @Modifying
    @Query("UPDATE SeedlingPlan sp SET sp.varietyName = :varietyName WHERE sp.varietyId = :varietyId")
    int updateVarietyNameByVarietyId(Long varietyId, String varietyName);
}
