package org.membershipcommons.entity.members;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.membershipcommons.constants.Constants;
import org.membershipcommons.entity.IdBasedEntity;
import org.membershipcommons.entity.State;
import org.membershipcommons.entity.finance.MemberInstitutionBalance;
import org.membershipcommons.entity.settings.FundingProject;
import org.membershipcommons.entity.settings.NeedType;
import org.membershipcommons.enums.MemberType;

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
@Table(name = "member_institutions")
public class MemberInstitution extends IdBasedEntity {
    @Column(length = 15, name = "phone_number")
    protected String phoneNumber;
    @Column(length = 10, name = "postal_code")
    protected String postalCode;
    @Column(length = 150, name = "postal_address")
    protected String postalAddress;
    @Column(name = "services_offered")
    protected String servicesOffered;
    @Enumerated(EnumType.STRING)
    private MemberType memberType;
    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @ManyToOne
    @JoinColumn(name = "province_id")
    private State province;
    @Column(name = "district", length = 45, nullable = false)
    private String district;
    @ManyToOne
    @JoinColumn(name = "level_of_care_id")
    private LevelOfCare levelOfCare;
    @Column(name = "catchment_population", nullable = false)
    private float catchmentPopulation;
    @ManyToOne
    @JoinColumn(name = "denomination_id")
    private Denomination denomination;
    private Date yearOpened;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private MemberAdmin contactPerson;
    @Column(name = "number_of_beds")
    private float numberOfBeds;
    @Column(length = 64)
    private String photos;
    @Column(length = 45)
    private String email;
    private boolean enabled;
    private boolean deleted;

    private float longitude;
    private float latitude;

    @ManyToMany
    @JoinTable(name = "member_needs", joinColumns = @JoinColumn(name = "member_id"), inverseJoinColumns = @JoinColumn(name = "need_type_id"))
    private Set<NeedType> memberNeeds = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "member_funding_projects", joinColumns = @JoinColumn(name = "member_id"), inverseJoinColumns = @JoinColumn(name = "funding_project_id"))
    private Set<FundingProject> memberFundingProjects = new HashSet<>();
    @Transient
    private MemberInstitutionBalance MemberInstitutionBalance;

    @Override
    public String toString() {
        return name + " ~ " + levelOfCare;
    }

    @Transient
    public String getPhotosImagePath() {
        if (id == null || photos == null) {
            return "/images/default_user_image.jpg";
        }
        return Constants.S3_BASE_URI + "/member_photos/" + this.id + "/" + this.photos;
    }

    @Transient
    public String getYOOnForm() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormatter.format(this.yearOpened);
    }

    public void setYOOnForm(String dateString) {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            this.yearOpened = dateFormatter.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
