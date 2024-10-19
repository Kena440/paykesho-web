package org.commons.entity.organization;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.commons.constants.Constants;
import org.commons.entity.IdBasedEntity;
import org.commons.enums.OrganizationType;

import javax.persistence.*;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "organizations")
public class Organization extends IdBasedEntity {

    @Column(length = 15, name = "phone_number")
    protected String phoneNumber;
    @Column(name = "name", length = 245, nullable = false, unique = true)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", length = 50)
    private OrganizationType organizationType;
    @Column(length = 45)
    private String email;
    @ManyToOne
    @JoinColumn(name = "contact_person_id")
    private OrganizationAdmin contactPerson;
    private boolean enabled;
    private boolean deleted;
    private float longitude;
    private float latitude;
    @Column(length = 64)
    private String photos;
    private Date registrationDate;
    @Column(name = "remote_file_id")
    private Integer remoteFileId;
    private String photoPath;

    @Override
    public String toString() {
        return name + " ~ " + organizationType;
    }

    @Transient
    public String getPhotosImagePath() {
        if (id == null || photos == null) {
            return "/images/default_image.png";
        }
        return Constants.S3_BASE_URI + "/organization_logos/" + this.id + "/" + this.photos;
    }
}
