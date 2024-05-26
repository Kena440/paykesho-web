package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_question_verification_responses")
public class AssessmentQuestionVerificationResponse extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_question_id")
    private AssessmentQuestion assessmentQuestion;
    @Column(name = "answer", length = 245, nullable = false)
    private String answer;
    @ManyToOne
    @JoinColumn(name = "assessment_interview_id")
    private AssessmentInterview assessmentInterview;

    @Override
    public String toString() {
        return answer;
    }
}
