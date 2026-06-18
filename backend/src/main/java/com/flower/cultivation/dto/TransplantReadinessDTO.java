package com.flower.cultivation.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransplantReadinessDTO {

    private Long sowingId;
    private String varietyName;
    private LocalDateTime sowingTime;
    private Integer seedlingDays;
    private Long actualSeedlingDays;
    private BigDecimal currentPlantHeight;
    private Integer currentLeafCount;
    private String currentRootDevelopment;
    private String currentHealthStatus;
    private LocalDateTime latestRecordTime;
    private ReadinessResult overallResult;
    private BigDecimal overallScore;
    private List<CriterionAssessment> criteria;

    @Data
    public static class CriterionAssessment {
        private String criterionName;
        private String criterionCode;
        private String actualValue;
        private String threshold;
        private BigDecimal score;
        private BigDecimal weight;
        private String assessment;
        private String suggestion;
        private boolean passed;
    }

    public enum ReadinessResult {
        READY("完全就绪", "已满足所有移栽条件，可立即移栽"),
        ALMOST_READY("基本就绪", "基本满足移栽条件，建议观察1-3天后移栽"),
        NOT_READY("未就绪", "暂不满足移栽条件，需要继续培育"),
        NEED_MORE_DATA("数据不足", "缺少必要的生长记录，无法进行评估");

        private final String displayName;
        private final String description;

        ReadinessResult(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }
}
