package org.chaz.portal.assessments.service;

import org.chaz.portal.assessments.repository.*;
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

    @Autowired
    private AssessmentsRepository assessmentsRepository;
    @Autowired
    private AssessmentQuestionsRepository assessmentQuestionsRepository;
    @Autowired
    private AssessmentInterviewsRepository assessmentInterviewsRepository;
    @Autowired
    private AssessmentFormsRepository assessmentFormsRepository;
    @Autowired
    private AssessmentQuestionOptionRepository assessmentQuestionOptionRepository;
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

    public AssessmentInterview saveInterview(AssessmentInterview assessmentInterview) {
        if (assessmentInterview == null) {
            return null;
        }

        return assessmentInterviewsRepository.save(assessmentInterview);
    }

    public AssessmentQuestionResponse saveInterviewResponse(AssessmentQuestionResponse assessmentQuestionResponse) {
        if (assessmentQuestionResponse == null) {
            return null;
        }

        return assessmentQuestionResponseRepository.save(assessmentQuestionResponse);
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
            throw new AssessmentToolError("Could not find any form");
        }
    }

    public AssessmentInterview getAssessmentInterviewById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentInterviewsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any form");
        }
    }

    public AssessmentQuestion getAssessmentQuestionById(Integer id) throws AssessmentToolError {
        if (id == null) {
            throw new AssessmentToolError("Null parameter id");
        }

        try {
            return assessmentQuestionsRepository.findById(id).get();
        } catch (NoSuchElementException exception) {
            throw new AssessmentToolError("Could not find any form");
        }
    }

    public List<AssessmentQuestion> getAssessmentQuestions(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestion> getAssessmentQuestionsByForm(AssessmentForm assessmentForm) throws AssessmentToolError {
        if (assessmentForm == null) {
            throw new AssessmentToolError("Null parameter assessmentForm");
        }

        return assessmentQuestionsRepository.findByAssessmentForm(assessmentForm.getId());
    }

    public List<AssessmentInterview> getAssessmentInterviews(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentInterviewsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentForm> getAssessmentForms(Assessment assessment) throws AssessmentToolError {
        if (assessment == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentFormsRepository.findByAssessment(assessment.getId());
    }

    public List<AssessmentQuestionResponse> getAssessmentQuestionResponsesByInterview(AssessmentInterview assessmentInterview) throws AssessmentToolError {
        if (assessmentInterview == null) {
            throw new AssessmentToolError("Null parameter assessment");
        }

        return assessmentQuestionResponseRepository.findByAssessmentInterview(assessmentInterview.getId());
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

    public List<AssessmentQuestionOption> getOptionsByQuestion(AssessmentQuestion question) {
        return assessmentQuestionOptionRepository.findByAssessmentQuestion(question.getId());
    }

    public List<Assessment> getUpcomingAssessments(int i) {
        if (i > 0) {
            return assessmentsRepository.findUpcomingAssessments(i);
        } else {
            return assessmentsRepository.findUpcomingAssessments(30);
        }
    }
}
