package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GerminationSummaryDTO {

    private Long sowingId;
    private String varietyName;
    private Integer sowingQuantity;
    private Integer totalGerminated;
    private Integer totalNotGerminated;
    private BigDecimal currentGerminationRate;
    private Integer observationDays;
    private List<GerminationObservationItem> observations;

    @Data
    public static class GerminationObservationItem {
        private Long id;
        private String observationDate;
        private Integer germinatedCount;
        private Integer notGerminatedCount;
        private BigDecimal germinationRate;
        private String anomalyNote;
    }
}
