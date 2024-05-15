package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_assessors")
public class AssessmentAssessor extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_id")
    private Assessment assessment;
    @ManyToOne
    @JoinColumn(name = "assessor_id")
    private Assessor assessor;
    @Column(name = "created_date")
    private Date createdDate;
    @Column(name = "assessment_reg_id", length = 245)
    private String assessmentRegId;
}
