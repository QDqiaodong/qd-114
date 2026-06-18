package com.flower.cultivation.repository;

import com.flower.cultivation.entity.TransplantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransplantRecordRepository extends JpaRepository<TransplantRecord, Long> {
    List<TransplantRecord> findBySowingId(Long sowingId);
    List<TransplantRecord> findAllByOrderByTransplantTimeDesc();
    boolean existsBySowingId(Long sowingId);
    boolean existsByVarietyId(Long varietyId);
    int countByVarietyId(Long varietyId);
    List<TransplantRecord> findBySowingIdAndIdNot(Long sowingId, Long id);
    List<TransplantRecord> findByVarietyId(Long varietyId);

    @Modifying
    @Query("UPDATE TransplantRecord t SET t.varietyName = :varietyName WHERE t.varietyId = :varietyId")
    int updateVarietyNameByVarietyId(Long varietyId, String varietyName);
}
