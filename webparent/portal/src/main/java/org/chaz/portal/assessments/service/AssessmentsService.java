package org.chaz.portal.assessments.service;

import org.chaz.portal.assessments.repository.AssessmentAssessorsRepository;
import org.chaz.portal.assessments.repository.AssessmentFeedbackRepository;
import org.chaz.portal.assessments.repository.AssessmentQuestionsRepository;
import org.chaz.portal.assessments.repository.AssessmentsRepository;
import org.chaz.portal.paging.PagingAndSortingHelper;
import org.commons.entity.assessment.*;
import org.commons.utils.AssessmentToolError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    @Autowired
    private AssessmentQuestionsRepository assessmentQuestionsRepository;

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

    public List<AssessmentQuestion> getAssessmentQuestions(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionsRepository.findByAssessment(assessment.getId());
    }

    public Page<Assessment> listForAssessorByPage(Assessor assessor, int pageNum, String sortField, String sortDir, String assessmentKeyword) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equals("asc") ? sort.ascending() : sort.descending();

        Pageable pageable = PageRequest.of(pageNum - 1, 10, sort);

        if (assessmentKeyword != null) {
            return assessmentsRepository.findAll(assessmentKeyword, assessor.getId(), pageable);
        }
        return assessmentsRepository.findAll(assessor.getId(), pageable);
    }
}
