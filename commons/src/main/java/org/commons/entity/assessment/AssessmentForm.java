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
@Table(name = "assessment_forms")
public class AssessmentForm extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;
    @Column(name = "form_name", length = 245, nullable = false)
    private String formName;
    @Column(name = "created_date")
    private Date createdDate;

}
