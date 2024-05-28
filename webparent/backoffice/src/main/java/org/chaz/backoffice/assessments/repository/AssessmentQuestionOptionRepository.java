package org.chaz.backoffice.assessments.repository;

import org.commons.entity.assessment.AssessmentQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssessmentQuestionOptionRepository extends JpaRepository<AssessmentQuestionOption, Integer> {

    @Query("select e from AssessmentQuestionOption e where e.question.id = ?1 order by e.id desc")
    public List<AssessmentQuestionOption> findByAssessmentQuestion(Integer eventId);

}
