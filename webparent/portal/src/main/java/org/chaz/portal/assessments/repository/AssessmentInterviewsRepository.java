package org.chaz.portal.assessments.repository;

import org.chaz.portal.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentInterview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface AssessmentInterviewsRepository extends SearchRepository<AssessmentInterview, Integer> {

    @Query("select e from AssessmentInterview e where e.assessmentForm.assessment.id = ?1 order by e.id desc")
    public List<AssessmentInterview> findByAssessment(Integer eventId);

    @Query("select e from AssessmentInterview e where concat(e.assessmentForm.assessment.name, ' ', e.assessmentForm.assessment.assessmentDate) like %?1%")
    public Page<AssessmentInterview> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    @Query("select e from AssessmentInterview e where e.assessmentForm.assessment.id = ?1 order by e.assessmentForm.assessment.assessmentDate desc")
    public Page<AssessmentInterview> findPageForAllByAssessment(Integer eventId, Pageable pageable);

    @Query("select e from AssessmentInterview e " +
            "where e.assessmentForm.assessment.id = ?1 " +
            "and e.createdDate >= ?2 " +
            "order by e.id desc")
    public List<AssessmentInterview> findRecentByAssessment(Integer assessmentId, Date twoDaysAgo);


    long countAssessmentInterviewsByAssessmentFormAssessmentId(Integer assessmentId);

    long countAssessmentInterviewsByAssessorIdAndAssessmentFormAssessmentId(Integer assessorId, Integer assessmentId);

    @Query("select e from AssessmentInterview e " +
            "where e.assessmentForm.assessment.id = ?2 " +
            "and e.assessor.id = ?1 " +
            "and e.createdDate >= ?3 " +
            "order by e.id desc")
    List<AssessmentInterview> findRecentByAssessmentAndAssessor(Integer assessorId, Integer assessmentId, Date date);
}
