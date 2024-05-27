package org.chaz.backoffice.assessments.repository;

import org.commons.entity.assessment.AssessmentQuestionVerificationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentQuestionVerificationResponseRepository extends JpaRepository<AssessmentQuestionVerificationResponse, Integer> {

    @Query("select e from AssessmentQuestionVerificationResponse e where e.assessmentQuestion.id = ?1 order by e.id desc")
    public List<AssessmentQuestionVerificationResponse> findByAssessmentQuestion(Integer eventId);

    @Query("select e from AssessmentQuestionVerificationResponse e where e.assessmentInterview.id = ?1 order by e.id desc")
    public List<AssessmentQuestionVerificationResponse> findByAssessmentInterview(Integer eventId);

}
