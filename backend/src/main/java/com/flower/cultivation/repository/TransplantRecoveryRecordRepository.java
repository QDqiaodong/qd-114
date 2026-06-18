package com.flower.cultivation.repository;

import com.flower.cultivation.entity.TransplantRecoveryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransplantRecoveryRecordRepository extends JpaRepository<TransplantRecoveryRecord, Long> {

    List<TransplantRecoveryRecord> findByTransplantIdOrderByRecordDateAsc(Long transplantId);

    List<TransplantRecoveryRecord> findByTransplantIdOrderByRecordDateDesc(Long transplantId);

    List<TransplantRecoveryRecord> findBySowingIdOrderByRecordDateAsc(Long sowingId);

    boolean existsByTransplantIdAndRecordDate(Long transplantId, LocalDate recordDate);

    TransplantRecoveryRecord findFirstByTransplantIdOrderByRecordDateDesc(Long transplantId);
}
