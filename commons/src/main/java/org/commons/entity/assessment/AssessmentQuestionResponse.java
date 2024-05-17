package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.QuestionType;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_question_responses")
public class AssessmentQuestionResponse extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_question_id")
    private AssessmentQuestion assessmentQuestion;
    @Column(name = "answer", length = 245, nullable = false)
    private String answer;
    @ManyToOne
    @JoinColumn(name = "assessment_interview_id")
    private AssessmentInterview assessmentInterview;
}
