package org.membershipcommons.entity.me;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.members.MemberAdmin;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "member_me_indicators")
public class MEIndicator extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;

    @Column(name = "skilled_deliveries")
    private float skilledDeliveries;

    @Column(name = "maternal_deaths")
    private float maternalDeaths;

    @Column(name = "percent_under_one_fully_immunised")
    private float percentUnderOneFullyImmunised;

    @Column(name = "malaria_incidence")
    private float malariaIncidence;

    @Column(name = "neoplasm")
    private float neoplasm;

    @Column(name = "hypertension")
    private float hypertension;

    @Column(name = "diabetes")
    private float diabetes;

    @Column(name = "in_patients")
    private float inPatients;

    @Column(name = "out_patients")
    private float outPatients;

    @Column(name = "government_grants")
    private float governmentGrants;

    @Column(name = "tss_chaz")
    private float tssChaz;

    private boolean reviewed;
    private Date period;
    private String submissionPeriod;

    @ManyToOne
    @JoinColumn(name = "creator_id")
    private MemberAdmin creator;
    private Date createdDate;

    //d.diabetes, d.governmentGrants, d.hypertension, d.inPatients, d.malariaIncidence, d.maternalDeaths, d.neoplasm, d.outPatients, d.percentUnderOneFullyImmunised, d.skilledDeliveries, d.tssChaz
    public MEIndicator(MemberInstitution memberInstitution, float diabetes, float governmentGrants, float hypertension, float inPatients, float malariaIncidence, float maternalDeaths, float neoplasm, float outPatients, float percentUnderOneFullyImmunised, float skilledDeliveries, float tssChaz) {
        this.memberInstitution = memberInstitution;
        this.skilledDeliveries = skilledDeliveries;
        this.maternalDeaths = maternalDeaths;
        this.percentUnderOneFullyImmunised = percentUnderOneFullyImmunised;
        this.malariaIncidence = malariaIncidence;
        this.neoplasm = neoplasm;
        this.hypertension = hypertension;
        this.diabetes = diabetes;
        this.inPatients = inPatients;
        this.outPatients = outPatients;
        this.governmentGrants = governmentGrants;
        this.tssChaz = tssChaz;
    }

    public MEIndicator(MemberInstitution memberInstitution, State state, float diabetes, float governmentGrants, float hypertension, float inPatients, float malariaIncidence, float maternalDeaths, float neoplasm, float outPatients, float percentUnderOneFullyImmunised, float skilledDeliveries, float tssChaz) {
        this.memberInstitution = memberInstitution;
        this.memberInstitution.setProvince(state);
        this.skilledDeliveries = skilledDeliveries;
        this.maternalDeaths = maternalDeaths;
        this.percentUnderOneFullyImmunised = percentUnderOneFullyImmunised;
        this.malariaIncidence = malariaIncidence;
        this.neoplasm = neoplasm;
        this.hypertension = hypertension;
        this.diabetes = diabetes;
        this.inPatients = inPatients;
        this.outPatients = outPatients;
        this.governmentGrants = governmentGrants;
        this.tssChaz = tssChaz;
    }

    public MEIndicator(Date period, MemberInstitution memberInstitution, float diabetes, float governmentGrants, float hypertension, float inPatients, float malariaIncidence, float maternalDeaths, float neoplasm, float outPatients, float percentUnderOneFullyImmunised, float skilledDeliveries, float tssChaz) {
        this.period = period;
        this.memberInstitution = memberInstitution;
        this.skilledDeliveries = skilledDeliveries;
        this.maternalDeaths = maternalDeaths;
        this.percentUnderOneFullyImmunised = percentUnderOneFullyImmunised;
        this.malariaIncidence = malariaIncidence;
        this.neoplasm = neoplasm;
        this.hypertension = hypertension;
        this.diabetes = diabetes;
        this.inPatients = inPatients;
        this.outPatients = outPatients;
        this.governmentGrants = governmentGrants;
        this.tssChaz = tssChaz;
    }

    @Override
    public String toString() {
        return memberInstitution.getName() + " ~ " + submissionPeriod;
    }

    @Transient
    public String getPeriodOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.period);
    }

    public void setPeriodOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.period = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
