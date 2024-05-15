package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.QuestionType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_form_id")
    private AssessmentForm assessmentForm;
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 30)
    private QuestionType questionType;
    @Column(name = "question", length = 245, nullable = false)
    private String question;
    private boolean skippable;
    @Column(name = "created_date")
    private Date createdDate;

}
