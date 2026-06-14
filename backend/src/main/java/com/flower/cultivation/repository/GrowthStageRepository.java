package com.flower.cultivation.repository;

import com.flower.cultivation.entity.GrowthStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrowthStageRepository extends JpaRepository<GrowthStage, Long> {
    List<GrowthStage> findAllByOrderByStageOrderAsc();
    Optional<GrowthStage> findByStageCode(String stageCode);
}
