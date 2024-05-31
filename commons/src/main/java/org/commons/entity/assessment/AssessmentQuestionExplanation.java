package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_question_explanations")
public class AssessmentQuestionExplanation extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_question_id")
    private AssessmentQuestion assessmentQuestion;
    @Column(name = "explanation", length = 245, nullable = false)
    private String explanation;
    @ManyToOne
    @JoinColumn(name = "assessment_interview_id")
    private AssessmentInterview assessmentInterview;

    @Override
    public String toString() {
        return explanation;
    }
}
