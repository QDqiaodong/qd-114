package com.flower.cultivation.repository;

import com.flower.cultivation.entity.SeedInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeedInfoRepository extends JpaRepository<SeedInfo, Long> {
    List<SeedInfo> findByVarietyId(Long varietyId);
    List<SeedInfo> findAllByOrderByCreateTimeDesc();

    @Query("SELECT COALESCE(SUM(s.remainingQuantity), 0) FROM SeedInfo s")
    Integer sumRemainingQuantity();

    List<SeedInfo> findByShelfLifeIsNotNullAndRemainingQuantityGreaterThanOrderByAcquireTimeAsc(Integer quantity);
}
