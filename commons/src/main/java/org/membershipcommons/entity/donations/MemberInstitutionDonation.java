package org.membershipcommons.entity.donations;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.members.MemberInstitution;

import javax.persistence.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
@Table(name = "member_donation")
public class MemberInstitutionDonation extends IdBasedEntity {

    @ManyToOne
    @JoinColumn(name = "member_institution_id")
    private MemberInstitution memberInstitution;
    @Column(name = "donor", nullable = false)
    private String donor;
    @Column(name = "comments", nullable = false)
    private String comments;
    private Date donationDate;
    private boolean confirmed;
    @OneToMany(mappedBy = "memberInstitutionDonation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MemberInstitutionDonationItem> donationItems = new HashSet<>();

    @Override
    public String toString() {
        return donor + " [" + donationDate + "]";
    }

    public void addItems(Integer id, String description, float amount) {
        this.donationItems.add(new MemberInstitutionDonationItem(id, description, amount, this));
    }

    public void addItems(String description, float amount) {
        this.donationItems.add(new MemberInstitutionDonationItem(description, amount, this));
    }

    @Transient
    public String getDdOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.donationDate);
    }

    public void setDdOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.donationDate = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
