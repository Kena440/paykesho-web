package org.chaz.portal.assessments.repository;

import org.commons.entity.assessment.AssessmentQuestionResponse;
import org.commons.entity.assessment.AssessmentQuestionVerificationResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentQuestionVerificationResponseRepository extends JpaRepository<AssessmentQuestionVerificationResponse, Integer> {

    @Query("select e from AssessmentQuestionResponse e where e.assessmentQuestion.id = ?1 order by e.id desc")
    public List<AssessmentQuestionVerificationResponse> findByAssessmentQuestion(Integer eventId);

    @Query("select e from AssessmentQuestionResponse e where e.assessmentInterview.id = ?1 order by e.id desc")
    public List<AssessmentQuestionVerificationResponse> findByAssessmentInterview(Integer eventId);
}
