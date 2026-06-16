package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SeedDetailDTO {

    private Long id;
    private Long varietyId;
    private String varietyName;
    private Integer remainingQuantity;
    private Integer initialQuantity;
    private String sourceType;
    private String sourceTypeName;
    private LocalDate acquireTime;
    private Integer shelfLife;
    private LocalDate expireDate;
    private String supplier;
    private String storageLocation;

    private Integer germinationDays;

    private GerminationHistory germinationHistory;

    @Data
    public static class GerminationHistory {
        private Integer totalSowings;
        private BigDecimal averageGerminationRate;
        private List<GerminationRecord> recentRecords;
    }

    @Data
    public static class GerminationRecord {
        private Long sowingId;
        private LocalDate sowingDate;
        private Integer sowingQuantity;
        private Integer estimatedSurvival;
        private BigDecimal germinationRate;
    }
}
