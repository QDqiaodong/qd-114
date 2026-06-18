package com.flower.cultivation.dto;

import lombok.Data;

@Data
public class TransplantEligibilityDTO {

    private Long sowingId;
    private boolean eligible;
    private String reason;
    private String currentStageCode;
    private String currentStageName;
    private Integer maxAllowedQuantity;
    private Integer transplantedQuantity;
    private Integer remainingQuantity;

    public static TransplantEligibilityDTO ineligible(Long sowingId, String reason, String currentStageCode, String currentStageName) {
        TransplantEligibilityDTO dto = new TransplantEligibilityDTO();
        dto.setSowingId(sowingId);
        dto.setEligible(false);
        dto.setReason(reason);
        dto.setCurrentStageCode(currentStageCode);
        dto.setCurrentStageName(currentStageName);
        return dto;
    }

    public static TransplantEligibilityDTO eligible(Long sowingId, String currentStageCode, String currentStageName,
                                                     int maxAllowedQuantity, int transplantedQuantity) {
        TransplantEligibilityDTO dto = new TransplantEligibilityDTO();
        dto.setSowingId(sowingId);
        dto.setEligible(true);
        dto.setCurrentStageCode(currentStageCode);
        dto.setCurrentStageName(currentStageName);
        dto.setMaxAllowedQuantity(maxAllowedQuantity);
        dto.setTransplantedQuantity(transplantedQuantity);
        dto.setRemainingQuantity(maxAllowedQuantity - transplantedQuantity);
        return dto;
    }
}
