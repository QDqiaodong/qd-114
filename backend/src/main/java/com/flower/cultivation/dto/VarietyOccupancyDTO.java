package com.flower.cultivation.dto;

import lombok.Data;

@Data
public class VarietyOccupancyDTO {

    private String moduleCode;
    private String moduleName;
    private Integer count;

    public VarietyOccupancyDTO() {}

    public VarietyOccupancyDTO(String moduleCode, String moduleName, Integer count) {
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.count = count;
    }
}
