package org.chaz.backoffice.assessments.controller;

import org.chaz.backoffice.assessments.service.AssessmentsService;
import org.chaz.backoffice.facilities.service.FacilityService;
import org.commons.entity.assessment.AssessmentForm;
import org.commons.entity.assessment.AssessmentInterview;
import org.commons.entity.assessment.AssessmentQuestion;
import org.commons.entity.assessment.AssessmentQuestionResponse;
import org.commons.enums.FormTarget;
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

    @GetMapping("/distribution/aggregated/{id}")
    public ResponseEntity<Map<String, Map<String, Long>>> getDistributionPerQuestionAggregated(@PathVariable("id") Integer id) {
        try {
            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestionsByForm(form);

            // Filter only MULTIPLE_CHOICE and CHECKBOX questions
            List<AssessmentQuestion> filteredQuestions = assessmentQuestions.stream().filter(q -> q.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE) || q.getQuestionType().equals(QuestionType.CHECKBOX)).collect(Collectors.toList());

            return getMapResponseEntity(assessmentInterviews, filteredQuestions);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/distribution/disaggregated/{id}")
    public ResponseEntity<Map<String, Map<String, Long>>> getDistributionPerQuestionDisaggregated(@PathVariable("id") Integer id) {
        try {
            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);
            List<AssessmentQuestion> assessmentQuestions = assessmentsService.getAssessmentQuestionsByForm(form);

            // Filter only MULTIPLE_CHOICE and CHECKBOX questions
            List<AssessmentQuestion> filteredQuestions = assessmentQuestions.stream()
                    .filter(q -> !q.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE) && !q.getQuestionType().equals(QuestionType.CHECKBOX) && !q.getQuestionType().equals(QuestionType.PRODUCT))
                    .collect(Collectors.toList());

            return getMapResponseEntity(assessmentInterviews, filteredQuestions);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    private ResponseEntity<Map<String, Map<String, Long>>> getMapResponseEntity(List<AssessmentInterview> assessmentInterviews, List<AssessmentQuestion> filteredQuestions) throws AssessmentToolError {
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
    }

    @GetMapping("/facility-types/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityTypeDistribution(@PathVariable("id") Integer id) {
        try {
            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);

            Map<String, Long> facilityTypeDistribution = assessmentInterviews.stream()
                    .filter(interview -> interview.getFacility() != null) // Filter out null facilities
                    .filter(interview -> interview.getFacility().getLevelOfCare() != null) // Filter out null levels of care
                    .filter(interview -> interview.getFacility().getLevelOfCare().getName() != null) // Filter out null names
                    .collect(Collectors.groupingBy(
                            interview -> interview.getFacility().getLevelOfCare().getName(),
                            Collectors.counting()
                    ));

            return ResponseEntity.ok(facilityTypeDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/facility-provinces/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityProvinceDistribution(@PathVariable("id") Integer id) {
        try {
            // Validate assessment ID
            if (id == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);

            // Filter out null values before grouping by province name
            Map<String, Long> facilityProvinceDistribution = assessmentInterviews.stream()
                    .filter(interview -> interview != null &&
                            interview.getFacility() != null &&
                            interview.getFacility().getProvince() != null &&
                            interview.getFacility().getProvince().getName() != null)
                    .collect(Collectors.groupingBy(interview -> interview.getFacility().getProvince().getName(), Collectors.counting()));

            return ResponseEntity.ok(facilityProvinceDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    @GetMapping("/facility-districts/{id}")
    public ResponseEntity<Map<String, Long>> getFacilityDistrictDistribution(@PathVariable("id") Integer id) {
        try {
            // Validate assessment ID
            if (id == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);

            // Filter out null values before grouping by district
            Map<String, Long> facilityDistrictDistribution = assessmentInterviews.stream()
                    .filter(interview -> interview != null &&
                            interview.getFacility() != null &&
                            interview.getFacility().getDistrict() != null)
                    .collect(Collectors.groupingBy(interview -> interview.getFacility().getDistrict(), Collectors.counting()));

            return ResponseEntity.ok(facilityDistrictDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/districts/{id}")
    public ResponseEntity<Map<String, Long>> getDistrictDistribution(@PathVariable("id") Integer id) {
        try {
            // Validate assessment ID
            if (id == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            AssessmentForm form = assessmentsService.getAssessmentFormById(id);
            List<AssessmentInterview> assessmentInterviews = assessmentsService.getAssessmentInterviewsByForm(form);

            // Filter out null values before grouping by district
            Map<String, Long> facilityDistrictDistribution = assessmentInterviews.stream()
                    .filter(interview -> interview != null &&
                            interview.getTargetDistrict() != null &&
                            interview.getAssessmentForm().getFormTarget() == FormTarget.DISTRICT)
                    .collect(Collectors.groupingBy(AssessmentInterview::getTargetDistrict, Collectors.counting()));

            return ResponseEntity.ok(facilityDistrictDistribution);
        } catch (AssessmentToolError e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }


    @GetMapping("/facility-response/{id}")
    public Map<String, Integer> getFacilityResponseData(@PathVariable("id") Integer assessmentFormId) {
        int totalFacilities = Math.toIntExact(facilityService.countInstitutions());
        int respondingFacilities = assessmentsService.getRespondingFacilities(assessmentFormId);

        Map<String, Integer> response = new HashMap<>();
        response.put("totalFacilities", totalFacilities);
        response.put("respondingFacilities", respondingFacilities);

        return response;
    }

    @GetMapping("/assessor-interviews/{id}")
    public Map<String, Long> getAssessorInterviews(@PathVariable("id") Integer assessmentFormId) {
        return assessmentsService.getInterviewsByAssessor(assessmentFormId);
    }

    @GetMapping("/completion-rates/{id}")
    public Map<String, Long> getCompletionRates(@PathVariable("id") Integer assessmentFormId) {
        return assessmentsService.getCompletionRates(assessmentFormId);
    }

    @GetMapping("/average-completion-time/{id}")
    public Map<String, String> getAverageCompletionTime(@PathVariable("id") Integer assessmentFormId) {
        return assessmentsService.getAverageCompletionTime(assessmentFormId);
    }
}
