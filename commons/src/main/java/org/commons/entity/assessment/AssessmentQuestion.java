package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.AuthenticationType;
import org.commons.enums.QuestionType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 30)
    private QuestionType questionType;
    @Column(name = "created_date")
    private Date createdDate;

}
