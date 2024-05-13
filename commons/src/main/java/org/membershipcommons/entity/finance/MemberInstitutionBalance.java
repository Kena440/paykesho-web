package org.membershipcommons.entity.finance;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "member_balance")
public class MemberInstitutionBalance extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @Column(name = "balance", nullable = false)
    private float balance;
    private Date lastUpdate;

    @Override
    public String toString() {
        return balance + " [" + lastUpdate + "]";
    }
}
