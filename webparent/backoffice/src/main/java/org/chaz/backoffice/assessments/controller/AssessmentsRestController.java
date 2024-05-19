package org.chaz.backoffice.assessments.controller;

import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.chaz.backoffice.facilities.service.FacilityService;
import org.commons.entity.assessment.Assessment;
import org.commons.entity.assessment.AssessmentInterview;
import org.commons.entity.assessment.AssessmentQuestion;
import org.commons.entity.assessment.AssessmentQuestionResponse;
import org.commons.enums.QuestionType;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assessments/rest")
public class AssessmentsRestController {

    @Autowired
    private AssessmentsService assessmentsService;
    @Autowired
    private FacilityService facilityService;

    @PostMapping("/check-assessment")
    public String checkDuplicateCode(Integer id, String name) {
        return assessmentsService.isAssessmentUnique(id, name) ? "OK" : "Duplicated";
    }

    @GetMapping("/distribution/{id}")
    public ResponseEntity<Map<String, Map<String, Long>>> getDistributionPerQuestion(@PathVariable("id") Integer id) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessment);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestions(assessment);

            // Filter only MULTIPLE_CHOICE and CHECKBOX questions
            List<AssessmentQuestion> filteredQuestions = assessmentQuestions.stream().filter(q -> q.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE) || q.getQuestionType().equals(QuestionType.CHECKBOX)).collect(Collectors.toList());

            Map<String, Map<String, Long>> distributionPerQuestion = new LinkedHashMap<>();
            for (AssessmentQuestion question : filteredQuestions) {
                Map<String, Long> distribution = new LinkedHashMap<>();
                for (AssessmentInterview interview : assessmentInterviews) {
                    List<AssessmentQuestionResponse> responses = assessmentsService.getAssessmentQuestionResponseByInterviewId(interview).stream().filter(response -> response.getAssessmentQuestion().getId().equals(question.getId())).collect(Collectors.toList());
                    for (AssessmentQuestionResponse response : responses) {
                        distribution.merge(response.getAnswer(), 1L, Long::sum);
                    }
                }
                distributionPerQuestion.put(question.getQuestion(), distribution);
            }

            return ResponseEntity.ok(distributionPerQuestion);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/facility-types/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityTypeDistribution(@PathVariable("id") Integer id) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessment);

            Map<String, Long> facilityTypeDistribution = assessmentInterviews.stream().collect(Collectors.groupingBy(interview -> interview.getFacility().getLevelOfCare().getName(), Collectors.counting()));

            return ResponseEntity.ok(facilityTypeDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/facility-provinces/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityProvinceDistribution(@PathVariable("id") Integer id) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessment);

            Map<String, Long> facilityProvinceDistribution = assessmentInterviews.stream().collect(Collectors.groupingBy(interview -> interview.getFacility().getProvince().getName(), Collectors.counting()));

            return ResponseEntity.ok(facilityProvinceDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/facility-districts/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityDistrictDistribution(@PathVariable("id") Integer id) {
        try {
            Assessment assessment = assessmentsService.getAssessmentById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviews(assessment);

            Map<String, Long> facilityProvinceDistribution = assessmentInterviews.stream().collect(Collectors.groupingBy(interview -> interview.getFacility().getDistrict(), Collectors.counting()));

            return ResponseEntity.ok(facilityProvinceDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/facility-response/{id}")
    public Map<String, Integer> getFacilityResponseData(@PathVariable("id") Integer assessmentId) {
        int totalFacilities = Math.toIntExact(facilityService.countInstitutions());
        int respondingFacilities = assessmentsService.getRespondingFacilities(assessmentId);

        Map<String, Integer> response = new HashMap<>();
        response.put("totalFacilities", totalFacilities);
        response.put("respondingFacilities", respondingFacilities);

        return response;
    }

    @GetMapping("/assessor-interviews/{id}")
    public Map<String, Long> getAssessorInterviews(@PathVariable("id") Integer assessmentId) {
        return assessmentsService.getInterviewsByAssessor(assessmentId);
    }
}
