package com.flower.cultivation.controller;

import com.flower.cultivation.common.Result;
import com.flower.cultivation.dto.TransplantDetailDTO;
import com.flower.cultivation.dto.TransplantRecoveryBoardDTO;
import com.flower.cultivation.entity.TransplantRecord;
import com.flower.cultivation.service.TransplantRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transplants")
@RequiredArgsConstructor
public class TransplantRecordController {

    private final TransplantRecordService transplantRecordService;

    @GetMapping
    public Result<List<TransplantRecord>> findAll() {
        return Result.success(transplantRecordService.findAll());
    }

    @GetMapping("/{id}")
    public Result<TransplantRecord> findById(@PathVariable Long id) {
        TransplantRecord record = transplantRecordService.findById(id);
        if (record == null) {
            return Result.fail("移栽记录不存在");
        }
        return Result.success(record);
    }

    @GetMapping("/{id}/detail")
    public Result<TransplantDetailDTO> findDetailById(@PathVariable Long id) {
        TransplantDetailDTO detail = transplantRecordService.findDetailById(id);
        if (detail == null) {
            return Result.fail("移栽记录不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/sowing/{sowingId}")
    public Result<List<TransplantRecord>> findBySowingId(@PathVariable Long sowingId) {
        return Result.success(transplantRecordService.findBySowingId(sowingId));
    }

    @PostMapping
    public Result<TransplantRecord> save(@RequestBody TransplantRecord record) {
        return Result.success(transplantRecordService.save(record));
    }

    @PutMapping("/{id}")
    public Result<TransplantRecord> update(@PathVariable Long id, @RequestBody TransplantRecord record) {
        record.setId(id);
        return Result.success(transplantRecordService.save(record));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        transplantRecordService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/recovery-board")
    public Result<TransplantRecoveryBoardDTO> getRecoveryBoard() {
        return Result.success(transplantRecordService.getRecoveryBoard());
    }
}
