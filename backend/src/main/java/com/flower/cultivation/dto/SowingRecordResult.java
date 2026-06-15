package com.flower.cultivation.dto;

import com.flower.cultivation.entity.SowingRecord;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SowingRecordResult {

    private SowingRecord sowingRecord;

    private Integer remainingQuantity;
}
