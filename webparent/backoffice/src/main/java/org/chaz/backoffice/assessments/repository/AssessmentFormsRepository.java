package org.chaz.backoffice.assessments.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentFormsRepository extends SearchRepository<AssessmentForm, Integer> {

    @Query("select e from AssessmentForm e where e.assessment.id = ?1 order by e.assessment.assessmentDate desc")
    public List<AssessmentForm> findByAssessment(Integer eventId);

    @Query("select e from AssessmentForm e where concat(e.assessment.name, ' ', e.assessment.assessmentDate) like %?1%")
    public Page<AssessmentForm> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    @Query("select e from AssessmentForm e where e.assessment.id = ?1 order by e.assessment.assessmentDate desc")
    public Page<AssessmentForm> findPageForAllByAssessment(Integer eventId, Pageable pageable);
}
