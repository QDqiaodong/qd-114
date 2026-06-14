package com.flower.cultivation.service;

import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.TransplantRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransplantRecordService {

    private final TransplantRecordRepository transplantRecordRepository;

    public List<TransplantRecord> findAll() {
        return transplantRecordRepository.findAllByOrderByTransplantTimeDesc();
    }

    public TransplantRecord findById(Long id) {
        return transplantRecordRepository.findById(id).orElse(null);
    }

    public List<TransplantRecord> findBySowingId(Long sowingId) {
        return transplantRecordRepository.findBySowingId(sowingId);
    }

    @Transactional
    public TransplantRecord save(TransplantRecord record) {
        return transplantRecordRepository.save(record);
    }

    @Transactional
    public void deleteById(Long id) {
        transplantRecordRepository.deleteById(id);
    }
}
