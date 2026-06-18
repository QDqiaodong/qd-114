package com.flower.cultivation.exception;

public class StageTransitionException extends RuntimeException {

    private final TransitionType transitionType;
    private final String currentStageName;
    private final String targetStageName;

    public enum TransitionType {
        DUPLICATE,
        BACKWARD,
        SKIP
    }

    public StageTransitionException(String message, TransitionType transitionType, String currentStageName, String targetStageName) {
        super(message);
        this.transitionType = transitionType;
        this.currentStageName = currentStageName;
        this.targetStageName = targetStageName;
    }

    public TransitionType getTransitionType() {
        return transitionType;
    }

    public String getCurrentStageName() {
        return currentStageName;
    }

    public String getTargetStageName() {
        return targetStageName;
    }
}
