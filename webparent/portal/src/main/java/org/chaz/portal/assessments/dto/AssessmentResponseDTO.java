package org.chaz.portal.assessments.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AssessmentResponseDTO {
    private String interviewId;
    private Map<String, Object> responses;

    // Getters and Setters
    public Map<String, Object> getResponses() {
        return responses;
    }

    public void setResponses(Map<String, Object> responses) {
        this.responses = responses;
    }
}
