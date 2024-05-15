package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_interviews")
public class AssessmentInterview extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_assessor_id")
    private AssessmentAssessor assessmentAssessor;
    @Column(name = "created_date")
    private Date createdDate;
}
