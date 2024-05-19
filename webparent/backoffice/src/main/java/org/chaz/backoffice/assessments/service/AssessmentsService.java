package org.chaz.backoffice.assessments.service;

import org.chaz.backoffice.assessments.repository.*;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.commons.entity.assessment.*;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentsService {

    private static final int EVENTS_PER_PAGE = 20;
    @Autowired
    private AssessmentsRepository assessmentsRepository;
    @Autowired
    private AssessmentAssessorsRepository assessmentAssessorsRepository;
    @Autowired
    private AssessmentFeedbackRepository assessmentFeedbackRepository;
    @Autowired
    private AssessmentQuestionsRepository assessmentQuestionsRepository;
    @Autowired
    private AssessmentFormsRepository assessmentFormsRepository;
    @Autowired
    private AssessmentQuestionOptionRepository assessmentQuestionOptionRepository;
    @Autowired
    private AssessmentInterviewsRepository assessmentInterviewsRepository;
    @Autowired
    private AssessmentQuestionResponseRepository assessmentQuestionResponseRepository;
    /**
     * +
     * This method saves assessment submitted from the assessment
     *
     * @param assessment
     */
    public Assessment save(Assessment assessment) {
        if (assessment == null) {
            return null;
        }

        return assessmentsRepository.save(assessment);
    }

    public AssessmentAssessor saveAssessor(AssessmentAssessor assessmentAssessor) throws AssessmentToolError {
        if (assessmentAssessor == null) {
            throw new AssessmentToolError("Null parameter Assessor");
        }

        return assessmentAssessorsRepository.save(assessmentAssessor);
    }

    public AssessmentQuestion saveQuestion(AssessmentQuestion assessmentQuestion) throws AssessmentToolError {
        if (assessmentQuestion == null) {
            throw new AssessmentToolError("Null parameter Question");
        }

        return assessmentQuestionsRepository.save(assessmentQuestion);
    }

    public AssessmentQuestionOption saveQuestionOption(AssessmentQuestionOption assessmentQuestionOption) throws AssessmentToolError {
        if (assessmentQuestionOption == null) {
            throw new AssessmentToolError("Null parameter Question");
        }

        return assessmentQuestionOptionRepository.save(assessmentQuestionOption);
    }


    public AssessmentForm saveForm(AssessmentForm assessmentForm) throws AssessmentToolError {
        if (assessmentForm == null) {
            throw new AssessmentToolError("Null parameter Form");
        }

        return assessmentFormsRepository.save(assessmentForm);
    }

    /**
     * +
     * This method checks if the code submitted is unique
     *
     * @param id
     * @param name
     * @return
     */
    public boolean isAssessmentUnique(Integer id, String name) {
        if (name == null) {
            return false;
        }
        Assessment assessment = assessmentsRepository.getAssessmentByName(name);

        if (assessment == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (assessment != null) return false;
        } else {
            if (assessment.getId() != id) {
                return false;
            }
        }

        return true;
    }

    public void listByPage(int pageNum, PagingAndSortingHelper helper) throws AssessmentToolError {
        helper.listEntities(pageNum, EVENTS_PER_PAGE, assessmentsRepository);
    }

    public Assessment getAssessmentById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public AssessmentForm getAssessmentFormById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentFormsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public AssessmentInterview getAssessmentInterviewById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentInterviewsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any Assessment");
        }
    }

    public void delete(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = assessmentsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Assessment");
        }

        assessmentsRepository.deleteById(id);
    }

    public void updateAssessmentStatus(Integer id, boolean enabled) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        Long countById = assessmentsRepository.countById(id);
        if (countById == null || countById == 0) {
            throw new AssessmentToolError("Could not find any Assessment");
        }

        assessmentsRepository.updateEnableStatus(id, enabled);
    }

    public List<Assessment> getUpcomingAssessments(int i) {
        if (i > 0) {
            return assessmentsRepository.findUpcomingAssessments(i);
        } else {
            return assessmentsRepository.findUpcomingAssessments(30);
        }
    }

    public List<AssessmentFeedback> getAssessmentFeedback(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentFeedbackRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentInterview> getAssessmentInterviews(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentInterviewsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentAssessor> getAssessmentAssessors(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentAssessorsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestion> getAssessmentQuestions(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentForm> getAssessmentForms(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentFormsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestionResponse> getAssessmentQuestionResponseByInterviewId(AssessmentInterview assessmentInterview) throws AssessmentToolError {
        if (assessmentInterview == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionResponseRepository.findByAssessmentInterview(assessmentInterview.getId());
    }

    public int getRespondingFacilities(Integer assessmentId) {
        List<AssessmentInterview> interviews = assessmentInterviewsRepository.findByAssessment(assessmentId);
        Set<Integer> uniqueFacilityIds = interviews.stream()
                .map(interview -> interview.getFacility().getId())
                .collect(Collectors.toSet());
        return uniqueFacilityIds.size();
    }

    public Map<String, Long> getInterviewsByAssessor(Integer assessmentId) {
        List<AssessmentInterview> interviews = assessmentInterviewsRepository.findByAssessment(assessmentId);
        return interviews.stream()
                .collect(Collectors.groupingBy(
                        interview -> interview.getAssessor().getName(),
                        Collectors.counting()
                ));
    }
}
