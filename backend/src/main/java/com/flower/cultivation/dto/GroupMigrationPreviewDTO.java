package com.flower.cultivation.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupMigrationPreviewDTO {

    private Long targetGroupId;
    private String targetGroupName;
    private String targetGroupCode;
    private String targetCategory;
    private String targetLocation;
    private Integer targetSortOrder;
    private Integer targetSeedCount;
    private Integer targetTotalQuantity;
    private Boolean categoryMatch;
    private String categoryWarning;
    private Integer suggestedSortOrder;
    private List<GroupSeedItemDTO> existingSeeds;
    private List<ConsumptionDistributionDTO> consumptionDistribution;
    private List<String> warnings;
}
