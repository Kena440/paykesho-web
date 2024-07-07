package org.commons.entity.assessment;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.entity.IdBasedEntity;

import javax.persistence.*;

@Data
@NoArgsConstructor
@Entity
@Table(name = "assessment_question_responses")
public class AssessmentQuestionResponse extends IdBasedEntity {
    @ManyToOne
    @JoinColumn(name = "assessment_question_id")
    private AssessmentQuestion assessmentQuestion;
    @Column(name = "answer", length = 245, nullable = false)
    private String answer;
    @ManyToOne
    @JoinColumn(name = "assessment_interview_id")
    private AssessmentInterview assessmentInterview;
    @Column(name = "managed_at_facility")
    private Boolean managedAtFacility;

    @Column(name = "soh_actual")
    private Integer sohActual;

    @Column(name = "soh_elmis_fe")
    private Integer sohElmisFe;

    @Column(name = "variance")
    private Integer variance;

    @Column(name = "percentage_error")
    private Double percentageError;

    @Column(name = "average_percentage_error")
    private Double averagePercentageError;

    //DQA PRODUCT FORM
    @Column(name = "monthly_physical_counts_physical_scc")
    private Integer monthlyPhysicalCountsPhysicalScc;

    @Column(name = "monthly_physical_counts_e_scc")
    private Integer monthlyPhysicalCountsEScc;

    @Column(name = "issues_in_last_3_months")
    private Integer issuesInLast3Months;

    //EQUIPMENT FORM
    @Column(name = "distributed_at_facility")
    private Boolean distributedAtFacility;

    @Column(name = "distributed_server")
    private Boolean distributedServer;

    @Column(name = "distributed_lab")
    private Boolean distributedLab;

    @Column(name = "distributed_art_dispensary")
    private Boolean distributedARTDispensary;

    @Column(name = "distributed_mch")
    private Boolean distributedMCH;

    @Column(name = "distributed_pharmacy_dispensary")
    private Boolean distributedPharmacyDispensary;

    @Override
    public String toString() {
        return answer;
    }
}
