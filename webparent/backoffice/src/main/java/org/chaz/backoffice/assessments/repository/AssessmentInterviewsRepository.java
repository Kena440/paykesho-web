package org.chaz.backoffice.assessments.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentInterview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

public interface AssessmentInterviewsRepository extends SearchRepository<AssessmentInterview, Integer> {

    @Query("select e from AssessmentInterview e where e.assessmentForm.assessment.id = ?1 order by e.id desc")
    public List<AssessmentInterview> findByAssessment(Integer eventId);

    @Query("select e from AssessmentInterview e " +
            "where e.assessmentForm.assessment.id = ?1 " +
            "and e.createdDate >= ?2 " +
            "order by e.id desc")
    public List<AssessmentInterview> findRecentByAssessment(Integer assessmentId, Date twoDaysAgo);


    @Query("select e from AssessmentInterview e where concat(e.createdDate, ' ', e.respondentName, ' ', e.respondentJobTitle, ' ', e.respondentPhoneNumber, ' ', e.facility.name, ' ', e.facility.levelOfCare.name, ' ', e.assessmentForm.formName, ' ', e.targetDistrict, ' ', e.assessmentForm.assessment.details) like %?1%")
    public Page<AssessmentInterview> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    @Query("select e from AssessmentInterview e where e.assessmentForm.assessment.id = ?1 order by e.assessmentForm.assessment.assessmentDate desc")
    public Page<AssessmentInterview> findPageForAllByAssessment(Integer eventId, Pageable pageable);

    long countByAssessmentFormId(Integer assessmentId);

    long countByAssessmentFormIdAndCompleted(Integer assessmentId, Boolean completed);

    long countAssessmentInterviewsByAssessmentFormAssessmentId(Integer assessmentId);


    List<AssessmentInterview> findAssessmentInterviewsByAssessmentFormId(Integer id);
}
