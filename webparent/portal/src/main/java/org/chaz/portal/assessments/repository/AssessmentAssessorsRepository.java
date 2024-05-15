package org.chaz.portal.assessments.repository;

import org.chaz.portal.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentAssessor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentAssessorsRepository extends SearchRepository<AssessmentAssessor, Integer> {

    @Query("select e from AssessmentAssessor e where concat(e.assessor.name, ' ', e.assessor.phoneNumber, ' ', e.assessor.email) like %?1% and e.assessor.id = ?2 and e.assessment.id = ?3")
    public Page<AssessmentAssessor> findAllByAssessorAndAssessment(String keyword, Integer assessorId, Integer assessmentId, Pageable pageable);

    @Query("select e from AssessmentAssessor e where e.assessor.id = ?1 and e.assessment.id = ?2 order by e.assessor.name desc")
    public List<AssessmentAssessor> findByAssessment(Integer memberId, Integer assessmentId);

    @Query("select e from AssessmentAssessor e where e.assessment.id = ?1 order by e.assessment.assessmentDate desc")
    public List<AssessmentAssessor> findByAssessment(Integer assessmentId);

    @Query("select e from AssessmentAssessor e where concat(e.assessor.name, ' ', e.assessor.phoneNumber) like %?1%")
    public Page<AssessmentAssessor> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    public AssessmentAssessor findByAssessmentRegId(String assessmentRegId);

    @Query("select e from AssessmentAssessor e where e.assessment.id = ?1 order by e.assessment.assessmentDate desc")
    public Page<AssessmentAssessor> findPageForAllByAssessment(Integer assessmentId, Pageable pageable);


}
