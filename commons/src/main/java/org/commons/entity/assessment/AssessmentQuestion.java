package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.QuestionType;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

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

    @Column(name = "needs_verification")
    private boolean needsVerification;

    @Column(name = "needs_explanation")
    private boolean needsExplanation;

    @Column(name = "verification_option", length = 500)
    private String verificationOption;

    @Column(name = "verification_question", length = 500)
    private String verificationQuestion;

    @Column(name = "verification_comment", length = 500)
    private String verificationComment;

    @Column(name = "created_date")
    private Date createdDate;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<AssessmentQuestionOption> options;

    @Override
    public String toString() {
        return question;
    }
}

