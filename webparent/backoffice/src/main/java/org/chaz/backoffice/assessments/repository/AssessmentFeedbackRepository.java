package org.chaz.backoffice.assessments.repository;

import org.chaz.backoffice.paging.SearchRepository;
import org.commons.entity.assessment.AssessmentFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentFeedbackRepository extends SearchRepository<AssessmentFeedback, Integer> {

    @Query("select e from AssessmentFeedback e where concat(e.assessmentAssessor.assessor.name, ' ', e.assessmentAssessor.assessor.phoneNumber, ' ', e.assessmentAssessor.assessor.email) like %?1% and e.assessmentAssessor.assessor.id = ?2 and e.assessmentAssessor.assessment.id = ?3")
    public Page<AssessmentFeedback> findAllByAssessmentAssessorAndAssessment(String keyword, Integer assessorId, Integer eventId, Pageable pageable);

    @Query("select e from AssessmentFeedback e where e.assessmentAssessor.assessor.id = ?1 and e.assessmentAssessor.assessment.id = ?2 order by e.assessmentAssessor.assessor.name desc")
    public List<AssessmentFeedback> findByAssessment(Integer memberId, Integer eventId);

    @Query("select e from AssessmentFeedback e where e.assessmentAssessor.assessment.id = ?1 order by e.assessmentAssessor.assessment.assessmentDate desc")
    public List<AssessmentFeedback> findByAssessment(Integer eventId);

    @Query("select e from AssessmentFeedback e where concat(e.assessmentAssessor.assessor.name, ' ', e.assessmentAssessor.assessor.phoneNumber) like %?1%")
    public Page<AssessmentFeedback> findAll(String keyword, Pageable pageable);

    public Long countById(Integer id);

    @Query("select e from AssessmentFeedback e where e.assessmentAssessor.assessment.id = ?1 order by e.assessmentAssessor.assessment.assessmentDate desc")
    public Page<AssessmentFeedback> findPageForAllByAssessment(Integer eventId, Pageable pageable);

}
