package org.membershipcommons.entity.initiative;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.util.Objects;

@Data
@NoArgsConstructor
@Entity
@Table(name = "initiative_members")
public class InitiativeMember extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "initiative_id")
    private Initiative initiative;
    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @Column(name = "money_expected_monthly", nullable = false)
    private float moneyExpectedMonthly;
    @Column(name = "money_remitted")
    private float moneyRemitted;
    @Column(name = "money_owing")
    private float moneyOwing;

    private boolean paid;

    public InitiativeMember(Integer id, float moneyExpectedMonthly, float moneyRemitted, float moneyOwing, MemberInstitution institution, Initiative initiative) {
        super();
        this.id = id;
        this.moneyExpectedMonthly = moneyExpectedMonthly;
        this.moneyRemitted = moneyRemitted;
        this.moneyOwing = moneyOwing;
        this.initiative = initiative;
        this.memberInstitution = institution;
    }

    public InitiativeMember(float moneyExpectedMonthly, float moneyRemitted, float moneyOwing, MemberInstitution institution, Initiative initiative) {
        this.moneyExpectedMonthly = moneyExpectedMonthly;
        this.moneyRemitted = moneyRemitted;
        this.moneyOwing = moneyOwing;
        this.initiative = initiative;
        this.memberInstitution = institution;

    }

    @Override
    public String toString() {
        return memberInstitution + " ~ " + moneyExpectedMonthly + " ~ " + moneyOwing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        InitiativeMember that = (InitiativeMember) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
