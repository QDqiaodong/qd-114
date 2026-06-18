package com.flower.cultivation.exception;

import com.flower.cultivation.dto.VarietyOccupancyDTO;

import java.util.List;

public class VarietyOccupiedException extends RuntimeException {

    private final List<VarietyOccupancyDTO> occupancies;

    public VarietyOccupiedException(String message, List<VarietyOccupancyDTO> occupancies) {
        super(message);
        this.occupancies = occupancies;
    }

    public List<VarietyOccupancyDTO> getOccupancies() {
        return occupancies;
    }
}
