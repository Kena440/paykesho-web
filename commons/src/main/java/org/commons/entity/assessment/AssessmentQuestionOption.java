package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_question_options")
public class AssessmentQuestionOption extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "question_id")
    private AssessmentQuestion question;

    @Column(name = "option_text", length = 255)
    private String optionText;

    @Override
    public String toString() {
        return optionText;
    }
}
