package org.chaz.backoffice.assessments.service;

import org.chaz.backoffice.assessments.repository.AssessmentAssessorsRepository;
import org.chaz.backoffice.assessments.repository.AssessmentFeedbackRepository;
import org.chaz.backoffice.assessments.repository.AssessmentsRepository;
import org.chaz.backoffice.paging.PagingAndSortingHelper;
import org.commons.entity.assessment.Assessment;
import org.commons.entity.assessment.AssessmentAssessor;
import org.commons.entity.assessment.AssessmentFeedback;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;

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

    /**
     * +
     * This method saves event submitted from the event
     *
     * @param event
     */
    public Assessment save(Assessment event) {
        if (event == null) {
            return null;
        }

        return assessmentsRepository.save(event);
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
        Assessment event = assessmentsRepository.getAssessmentByName(name);

        if (event == null) return true;
        boolean isCreatingNew = (id == null);

        if (isCreatingNew) {
            if (event != null) return false;
        } else {
            if (event.getId() != id) {
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
            throw new AssessmentToolError("Null parameter event");
        }

        return assessmentFeedbackRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentAssessor> getAssessmentAssessors(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter event");
        }

        return assessmentAssessorsRepository.findByAssessment(assessment.getId());
    }
}
