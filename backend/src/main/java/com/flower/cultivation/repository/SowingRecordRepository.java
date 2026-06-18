package com.flower.cultivation.repository;

import com.flower.cultivation.entity.SowingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SowingRecordRepository extends JpaRepository<SowingRecord, Long> {
    List<SowingRecord> findBySeedId(Long seedId);
    List<SowingRecord> findByVarietyId(Long varietyId);
    List<SowingRecord> findAllByOrderBySowingTimeDesc();
    List<SowingRecord> findByIdNotInOrderBySowingTimeDesc(List<Long> ids);
    boolean existsBySeedId(Long seedId);
    boolean existsByVarietyId(Long varietyId);
    int countByVarietyId(Long varietyId);
}
