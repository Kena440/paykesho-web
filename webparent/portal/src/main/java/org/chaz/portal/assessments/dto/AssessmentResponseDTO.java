package org.chaz.portal.assessments.dto;

import java.util.Map;

public class AssessmentResponseDTO {
    private Map<String, String> responses;

    // Getters and Setters
    public Map<String, String> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, String> responses) {
        this.responses = responses;
    }
}
