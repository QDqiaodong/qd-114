package com.flower.cultivation.service;

import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.repository.SowingRecordRepository;
import com.flower.cultivation.repository.TransplantRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransplantRecordService {

    private final TransplantRecordRepository transplantRecordRepository;
    private final SowingRecordRepository sowingRecordRepository;

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
        validateTransplantRecord(record);
        return transplantRecordRepository.save(record);
    }

    @Transactional
    public void deleteById(Long id) {
        transplantRecordRepository.deleteById(id);
    }

    private void validateTransplantRecord(TransplantRecord record) {
        SowingRecord sowing = sowingRecordRepository.findById(record.getSowingId()).orElse(null);
        if (sowing == null) {
            throw new RuntimeException("播种记录不存在");
        }

        if (record.getTransplantTime() != null && record.getTransplantTime().isBefore(sowing.getSowingTime())) {
            throw new RuntimeException("移栽时间不能早于播种时间");
        }

        List<TransplantRecord> existingRecords;
        if (record.getId() == null) {
            existingRecords = transplantRecordRepository.findBySowingId(record.getSowingId());
        } else {
            existingRecords = transplantRecordRepository.findBySowingIdAndIdNot(record.getSowingId(), record.getId());
        }

        int totalTransplanted = existingRecords.stream()
                .mapToInt(TransplantRecord::getTransplantQuantity)
                .sum();

        if (record.getTransplantQuantity() == null || record.getTransplantQuantity() <= 0) {
            throw new RuntimeException("移栽数量必须大于0");
        }

        if (totalTransplanted + record.getTransplantQuantity() > sowing.getSowingQuantity()) {
            throw new RuntimeException(
                    String.format("移栽总数(%d)不得超过播种数量(%d)，当前已移栽%d棵",
                            totalTransplanted + record.getTransplantQuantity(),
                            sowing.getSowingQuantity(),
                            totalTransplanted)
            );
        }
    }
}
