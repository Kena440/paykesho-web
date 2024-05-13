package org.membershipcommons.entity.initiative;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.User;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "initiatives")
public class Initiative extends IdBasedEntity {
    @Column(length = 64, nullable = false)
    private String name;
    @Column(length = 150, nullable = false)
    private String details;
    @Column(name = "maturity", nullable = false)
    private float maturity;
    private boolean enabled;
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User creator;
    private Date createdDate;

    @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InitiativeMember> initiativeMembers = new HashSet<>();

    @OneToMany(mappedBy = "initiative", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<InitiativePaymentItem> initiativePaymentItems = new HashSet<>();

    public Initiative(String name, String details) {
        this.name = name;
        this.details = details;
    }

    public void addRepaymentItem(Integer id, Date paymentDate, String description, float amount, MemberInstitution institution) {
        this.initiativePaymentItems.add(new InitiativePaymentItem(id, paymentDate, description, amount, institution, this));
    }

    public void addRepaymentItem(Date paymentDate, String description, float amount, MemberInstitution institution) {
        this.initiativePaymentItems.add(new InitiativePaymentItem(paymentDate, description, amount, institution, this));
    }

    @Override
    public String toString() {
        return name;
    }
}
