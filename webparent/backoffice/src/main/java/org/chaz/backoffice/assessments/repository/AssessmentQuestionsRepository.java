package org.chaz.backoffice.assessments.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentQuestionsRepository extends SearchRepository<AssessmentQuestion, Integer> {

    @Query("select e from AssessmentQuestion e where e.assessmentForm.assessment.id = ?1 order by e.assessmentForm.assessment.assessmentDate desc")
    public List<AssessmentQuestion> findByAssessment(Integer assessmentId);

    @Query("select e from AssessmentQuestion e where e.assessmentForm.id = ?1 order by e.assessmentForm.assessment.assessmentDate desc")
    public List<AssessmentQuestion> findByAssessmentForm(Integer assessmentId);

    @Query("select e from AssessmentQuestion e where e.assessmentForm.id = ?1 and e.resultsQuestion = ?2 order by e.assessmentForm.assessment.assessmentDate desc")
    public List<AssessmentQuestion> findByAssessmentFormAndResultsQuestion(Integer assessmentId, boolean status);

    @Query("select e from AssessmentQuestion e where concat(e.assessmentForm.assessment.name, ' ', e.assessmentForm.assessment.assessmentDate) like %?1%")
    public Page<AssessmentQuestion> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    @Query("select e from AssessmentQuestion e where e.assessmentForm.assessment.id = ?1 order by e.assessmentForm.assessment.assessmentDate desc")
    public Page<AssessmentQuestion> findPageForAllByAssessment(Integer assessmentId, Pageable pageable);
}
