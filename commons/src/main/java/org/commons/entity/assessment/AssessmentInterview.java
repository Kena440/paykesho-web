package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;
import org.commons.entity.facilities.Facility;

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
    @ManyToOne
    @JoinColumn(name = "facility_id")
    private Facility facility;
    @Column(name = "respondent_name", length = 245, nullable = false)
    private String respondentName;
    @Column(name = "respondent_phone_number", length = 20, nullable = false)
    private String respondentPhoneNumber;
    @Column(name = "respondent_job_title", length = 245, nullable = false)
    private String respondentJobTitle;
    @Column(name = "start_time", length = 20, nullable = false)
    private String startTime;
    @Column(name = "end_time", length = 20, nullable = false)
    private String endTime;
    @Column(name = "created_date")
    private Date createdDate;
}
