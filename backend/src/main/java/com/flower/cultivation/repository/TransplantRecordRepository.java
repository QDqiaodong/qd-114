package com.flower.cultivation.repository;

import com.flower.cultivation.entity.TransplantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransplantRecordRepository extends JpaRepository<TransplantRecord, Long> {
    List<TransplantRecord> findBySowingId(Long sowingId);
    List<TransplantRecord> findAllByOrderByTransplantTimeDesc();
}
