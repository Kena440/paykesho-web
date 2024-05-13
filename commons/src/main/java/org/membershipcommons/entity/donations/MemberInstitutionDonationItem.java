package org.membershipcommons.entity.donations;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;

import javax.persistence.*;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "member_donation_items")
public class MemberInstitutionDonationItem extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_donation_id")
    private MemberInstitutionDonation memberInstitutionDonation;
    @Column(name = "description", length = 245, nullable = false)
    private String description;
    @Column(name = "amount", nullable = false)
    private float amount;

    public MemberInstitutionDonationItem(Integer id, String description, float amount, MemberInstitutionDonation memberInstitutionDonation) {
        super();
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.memberInstitutionDonation = memberInstitutionDonation;
    }

    public MemberInstitutionDonationItem(String description, float amount, MemberInstitutionDonation memberInstitutionDonation) {
        ;
        this.description = description;
        this.amount = amount;
        this.memberInstitutionDonation = memberInstitutionDonation;
    }


    @Override
    public String toString() {
        return description + " ~ " + amount;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        MemberInstitutionDonationItem that = (MemberInstitutionDonationItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
